package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.DataSetTagFactory;
import life.qbic.datamanager.views.general.DataSetTagFactory.TagType;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetServiceException;
import life.qbic.projectmanagement.application.associated_dataset.DatasetAccessFilter;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
import life.qbic.projectmanagement.application.associated_dataset.SearchHit;
import life.qbic.projectmanagement.application.associated_dataset.SourceInstanceDescriptor;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Connect Dataset Sidebar</b>
 *
 * <p>A right-side sliding panel that overlays the associated-datasets
 * view. Allows users to:
 * <ul>
 *   <li>select which InvenioRDM instance to search in (ComboBox);</li>
 *   <li>search for open datasets via InvenioRDM's REST API (TextField
 *       with Enter-key trigger);</li>
 *   <li>select one or more search results via a multi-select grid with
 *       card-style rows;</li>
 *   <li>optionally pick an experiment to associate each selected dataset
 *       with;</li>
 *   <li>confirm the connection via a footer button.</li>
 * </ul>
 *
 * <p>The sidebar is instantiated lazily by the parent view and opened
 * via {@link #open()}. It closes itself after a successful connect or
 * when the user clicks the close button / backdrop.</p>
 *
 * <p>Fires {@link DatasetsConnectedEvent} on a successful connect so the
 * parent view can refresh the connected-resources grid.</p>
 *
 * @since 1.12.0
 */
public class ConnectDatasetSidebar extends Div {

  private static final Logger log = LoggerFactory.getLogger(ConnectDatasetSidebar.class);

  private final AssociatedDatasetService associatedDatasetService;
  private final ExperimentInformationService experimentInformationService;
  private final MessageSourceNotificationFactory notificationFactory;
  private final ExternalCredentialService externalCredentialService;
  private final UiHandle uiHandle = new UiHandle();
  private final AuthenticationToUserIdTranslator authenticationToUserIdTranslator;

  /**
   * Clamp a title to {@code maxLines} lines with a CSS ellipsis.
   * The full title is exposed as a native HTML {@code title} attribute so hovering reveals
   * it even when truncated.
   *
   * <p>{@code maxLines} must be 1, 2, or 3 (matching the {@code .clamp-N-line}
   * CSS utility classes defined in {@code all.css}).</p>
   */
  private Span clampableTitle(String title, int maxLines) {
    var span = new Span(title);
    span.addClassName("normal-body-text");
    span.addClassName("clamp-" + maxLines + "-line");
    span.getElement().setAttribute("title", title);
    return span;
  }

  @Serial
  private static final long serialVersionUID = 1L;

  private final Div overlay;
  private final Div panel;
  private final ComboBox<SourceInstanceDescriptor> instanceSelector;
  private final TextField searchField;
  private Button searchButton;
  private final Grid<SearchHit> resultsGrid;
  private final ComboBox<ExperimentEntry> experimentSelector;
  private final Button connectButton;
  private final Span selectionCountLabel;

  /**
   * Wraps {@code resultsGrid} together with {@code loadingIndicator} and
   * {@code welcomeMessage}. The spinner is a sibling (not a CSS overlay)
   * because the grid is already the scroll container; we just swap
   * visibility. The welcome message is absolutely positioned over the
   * empty grid until the user initiates a search.
   */
  private final Div resultsContainer;
  private final Div loadingIndicator;
  private final Span loadingMessage = new Span("Searching for datasets...");
  private final Div welcomeMessage;

  // ── Access filter toggle (segmented button pair) ──────────────────
  private final Button allDatasetsButton;
  private final Button restrictedOnlyButton;
  /** null = all datasets, RESTRICTED = restricted only */
  private DatasetAccessFilter accessFilter = null;

  // ── Credential banner (search-time, informational) ────────────────
  // Shown when the user has no valid provider connection for the selected
  // instance. Non-blocking — the search proceeds regardless, but the
  // banner explains why connecting restricted datasets will fail.
  private final Div credentialBanner;
  private final Button configureConnectionButton;

  /**
   * Tracks which dataset cards have inline errors (e.g., permission denied).
   * Key: externalHandleValue, Value: error message
   */
  private final Map<String, String> cardErrors = new HashMap<>();

  /**
   * Gate for the lazy-loading callback.
   *
   * <p>Vaadin calls {@link #fetchPage(Query)} automatically whenever the
   * grid first renders (to fill initially-visible rows) or when it needs
   * a page during scroll. If the sidebar is being opened (the grid was
   * just attached to the UI), this automatic first-page fetch would fire
   * an HTTP call against InvenioRDM from the same Vaadin response that
   * is supposed to reveal the sidebar — making the open action appear
   * to freeze.
   *
   * <p>To keep {@code open()} instant, the flag is cleared in
   * {@link #close()} and set to {@code true} only when the user has
   * explicitly triggered a search (by clicking Search, pressing Enter,
   * or clearing the field). Until then, {@code fetchPage} returns an
   * empty stream. After the first explicit search the gate stays open
   * for the lifetime of that sidebar session, so scrolling still loads
   * follow-up pages lazily.
   *
   * <p>This does not affect AC3 ("Paginated results when no query") —
   * the user's very first search can be with an empty term.
   */
  private volatile boolean searchInitiated = false;

  /**
   * Tracks whether a search is currently in progress (async HTTP fetch).
   * Used to disable controls and prevent duplicate requests.
   */
  private volatile boolean searchInProgress = false;

  /**
   * Cached search results from the most recent successful search.
   * Populated by the background thread in {@link #refreshSearchResults()},
   * then sliced on-demand by {@link #fetchPage(Query)} for virtual scrolling.
   */
  private final List<SearchHit> cachedResults = new ArrayList<>();

  private final List<SourceInstanceDescriptor> availableInstances = new ArrayList<>();
  private final List<ExperimentEntry> availableExperiments = new ArrayList<>();

  private Context context;

  public ConnectDatasetSidebar(
      AssociatedDatasetService associatedDatasetService,
      ExperimentInformationService experimentInformationService,
      MessageSourceNotificationFactory notificationFactory,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator,
      ExternalCredentialService externalCredentialService) {
    this.associatedDatasetService = associatedDatasetService;
    this.experimentInformationService = experimentInformationService;
    this.notificationFactory = requireNonNull(notificationFactory,
        "notificationFactory must not be null");
    this.authenticationToUserIdTranslator = requireNonNull(authenticationToUserIdTranslator);
    this.externalCredentialService = requireNonNull(externalCredentialService,
        "externalCredentialService must not be null");

    // Root scope class — all .cds-* CSS rules in connect-dataset-sidebar.css
    // are scoped under this class so they cannot leak into other views.
    addClassName("connect-dataset-sidebar");

    // ── Form controls (MUST be initialized before buildSidebarBody) ──
    // Some fields are referenced directly inside buildSidebarBody, which is
    // called at the end of this constructor. Initializing them up front
    // avoids a NullPointerException that would otherwise fire the moment
    // the sidebar is built.
    instanceSelector = new ComboBox<>();
    instanceSelector.setLabel("Repository");
    instanceSelector.setPlaceholder("Select repository…");
    instanceSelector.setItemLabelGenerator(SourceInstanceDescriptor::displayName);
    instanceSelector.addValueChangeListener(e -> {
      // Always update the credential banner when instance changes
      updateCredentialBanner();

      // Only refetch when the user changes the instance *after* an
      // explicit search. During open(), loadInstances() auto-selects the
      // first item — we must NOT treat that auto-selection as a search
      // trigger, or the sidebar would block on InvenioRDM latency.
      if (!searchInitiated) {
        return;
      }
      refreshSearchResults();
    });
    instanceSelector.setOverlayClassName("connect-dataset-sidebar-overlay");

    searchField = new TextField();
    searchField.setPlaceholder("Search by title, DOI, or creator…");
    searchField.setClearButtonVisible(true);
    searchField.addClassName("flex-grow");
    searchField.addKeyDownListener(Key.ENTER, e -> refreshSearchResults());

    resultsGrid = new Grid<>();
    resultsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_ROW_BORDERS);
    resultsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
    resultsGrid.setWidthFull();
    resultsGrid.setHeight("400px"); // Fixed height to enable virtual scrolling
    resultsGrid.addComponentColumn(this::buildSearchResultCard).setFlexGrow(1);
    resultsGrid.addSelectionListener(e -> onSelectionChanged());
    // Clicking anywhere on a row (not just the checkbox) toggles selection.
    // The checkbox still works independently. Cursor pointer is applied
    // per-card in buildSearchResultCard so the affordance is scoped to the
    // card content, not the whole grid.
    resultsGrid.addItemClickListener(e -> {
      SearchHit hit = e.getItem();
      if (resultsGrid.getSelectedItems().contains(hit)) {
        resultsGrid.deselect(hit);
      } else {
        resultsGrid.select(hit);
      }
    });

    // ── Lazy-loading data provider ────────────────────────────────
    // The grid fetches pages on demand as the user scrolls, rather than
    // pulling the first page synchronously when the sidebar opens. This
    // keeps the UI responsive regardless of InvenioRDM latency.
    resultsGrid.setItems(this::fetchPage);

    // ── Loading indicator ──────────────────────────────────────────
    // Indeterminate progress bar + "Searching…" label, shown during
    // lazy fetches. Wrapped together with the grid in resultsContainer.
    // Note: without @Push the bar is rendered in the same response as
    // the data so it flashes briefly, but it still gives the user a
    // visible cue that a fetch is in progress on the server side.
    // Indeterminate spinner + clear messaging, shown during searches.
    // Spinner size, colors and animation live entirely in CSS (pure-CSS
    // arc spinner — Spinner II, Temani Afif); the DOM element is an empty
    // div carrying only the .cds-loading-spinner class.
    var spinner = new Div();
    spinner.addClassName("cds-loading-spinner");
    
    loadingMessage.addClassName("normal-body-text");
    loadingMessage.addClassName("cds-loading-message");
    
    var loadingHint = new Span("This may take a few seconds");
    loadingHint.addClassName("small-body-text");
    loadingHint.addClassName("cds-loading-hint");

    // The loading overlay uses .overlay-center-fill for the absolute-fill
    // + centred-positioning primitives (see all.css); .cds-loading adds
    // the base-colour backdrop and z-index (see connect-dataset-sidebar.css).
    loadingIndicator = new Div();
    loadingIndicator.addClassNames(
        "flex-vertical", "items-center", "gap-02",
        "overlay-center-fill", "cds-loading");
    loadingIndicator.add(spinner, loadingMessage, loadingHint);

    // ── Welcome message (shown before first search) ──────────────────
    // Same absolute-fill + centred-positioning as the loading overlay;
    // .cds-welcome adds its own colour scheme and z-index.
    welcomeMessage = new Div();
    welcomeMessage.addClassNames(
        "flex-vertical", "items-center", "gap-02",
        "overlay-center-fill", "cds-welcome");
    
    var welcomeIcon = VaadinIcon.SEARCH.create();
    welcomeIcon.addClassName("cds-welcome-icon");
    
    var welcomeTitle = new Span("Search for datasets");
    welcomeTitle.addClassName("heading-4");
    welcomeTitle.addClassName("mt-s");
    
    var welcomeSubtitle = new Span(
        "Use the search field above to find open datasets you can connect to this project.");
    welcomeSubtitle.addClassName("body-text");
    welcomeSubtitle.addClassName("cds-welcome-subtitle");
    
    welcomeMessage.add(welcomeIcon, welcomeTitle, welcomeSubtitle);

    // .cds-results sets position:relative (the parent for the absolute
    // overlays above) and min-height:200px so the grid always has room
    // to display at least one card row even before data has loaded.
    resultsContainer = new Div();
    resultsContainer.addClassNames("flex-vertical", "flex-grow", "cds-results");
    resultsContainer.add(loadingIndicator, welcomeMessage, resultsGrid);

    experimentSelector = new ComboBox<>();
    experimentSelector.setLabel("Link to experiment (optional)");
    experimentSelector.setPlaceholder("No experiment selected");
    experimentSelector.setItems(availableExperiments);
    experimentSelector.setItemLabelGenerator(ExperimentEntry::label);
    experimentSelector.setClearButtonVisible(true);
    experimentSelector.setWidthFull();
    experimentSelector.setOverlayClassName("connect-dataset-sidebar-overlay");

    selectionCountLabel = new Span();
    selectionCountLabel.addClassName("normal-body-text");
    selectionCountLabel.addClassName("color-secondary");
    // Start hidden — an empty Span still reserves line-height space and
    // would push the button right of the footer's left alignment.
    selectionCountLabel.getStyle().set("display", "none");

    connectButton = new Button("Connect Selected", VaadinIcon.PLUS_CIRCLE.create());
    connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectButton.setEnabled(false);
    connectButton.addClickListener(e -> connectSelectedDatasets());

    // ── Access filter toggle (segmented button pair) ──────────────
    // Two buttons styled as a segmented control. Clicking one activates
    // it and deactivates the other. The active button gets a filled
    // primary style; the inactive one gets a tertiary outline style.
    allDatasetsButton = new Button("All datasets");
    allDatasetsButton.addClassName("cds-segmented-btn");
    allDatasetsButton.addClassName("cds-segmented-btn--active");
    allDatasetsButton.addClickListener(e -> {
      if (accessFilter == null) return; // already active
      accessFilter = null;
      updateToggleState();
      if (searchInitiated) refreshSearchResults();
    });

    restrictedOnlyButton = new Button("Restricted only");
    restrictedOnlyButton.addClassName("cds-segmented-btn");
    restrictedOnlyButton.addClickListener(e -> {
      if (DatasetAccessFilter.RESTRICTED.equals(accessFilter)) return; // already active
      accessFilter = DatasetAccessFilter.RESTRICTED;
      updateToggleState();
      if (searchInitiated) refreshSearchResults();
    });

    //  Credential banner (search-time, informational) ────────────
    // Shown above the results container when the user has no valid
    // provider connection for the selected instance. Non-blocking —
    // the search proceeds regardless, but the banner explains why
    // connecting restricted datasets will fail. Hidden when a valid
    // connection exists.
    credentialBanner = new Div();
    credentialBanner.addClassNames("cds-credential-banner");
    credentialBanner.getStyle().set("display", "none");

    // ─ Connect error banner (shown when user tries to connect restricted
    // datasets without a valid connection) ───────────────────────────────
    configureConnectionButton = new Button("Configure Connection", VaadinIcon.EXTERNAL_LINK.create());
    configureConnectionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    configureConnectionButton.addClassName("cds-credential-banner-button");
    configureConnectionButton.addClickListener(e ->
        UI.getCurrent().getPage().open(
            com.vaadin.flow.router.RouteConfiguration.forSessionScope()
                .getUrl(life.qbic.datamanager.views.account.ExternalProvidersMain.class),
            "_blank"));

    // ── Overlay (semi-transparent backdrop) ──────────────────────────
    // .cds-backdrop carries all visual properties; display toggling stays
    // inline because it represents runtime open/close state, not layout.
    overlay = new Div();
    overlay.addClassName("cds-backdrop");
    overlay.getStyle().set("display", "none");
    overlay.addClickListener(e -> close());
    add(overlay);

    // ── Panel ─────────────────────────────────────────────────────────
    // .cds-panel carries width, shadow, z-index, etc.; display toggling
    // stays inline (same rationale as overlay above).
    panel = new Div();
    panel.addClassName("cds-panel");
    panel.getStyle().set("display", "none");
    add(panel);

    // Build sidebar body AFTER form-controls are initialized — it reads
    // instanceSelector / searchField / resultsGrid / experimentSelector /
    // selectionCountLabel / connectButton directly when composing the DOM.
    panel.add(buildSidebarBody());
  }

  // ── Public API ──────────────────────────────────────────────────────

  public void setContext(Context context) {
    this.context = context;
    if (context != null && context.projectId().isPresent()) {
      loadInstances();
    }
  }

  public void open() {
    // Bind UI context for async operations
    uiHandle.bind(UI.getCurrent());
    
    // Reset UI state to ensure welcome message is visible
    searchInitiated = false;
    loadingIndicator.getStyle().set("display", "none");
    welcomeMessage.getStyle().set("display", "flex");
    resultsGrid.getDataProvider().refreshAll();
    setControlsEnabled(true);
    
    loadInstances();
    // Load experiments asynchronously to avoid blocking
    if (context != null && context.projectId().isPresent()) {
      loadExperimentsAsync();
    }
    overlay.getStyle().set("display", "block");
    panel.getStyle().set("display", "block");
    // We intentionally do NOT trigger any fetch here. The grid's lazy-
    // loading data provider is gated by {@link #searchInitiated} — until
    // the user clicks Search / presses Enter / clicks Clear, fetchPage()
    // returns an empty stream without making an HTTP call. That is what
    // keeps this method non-blocking even when InvenioRDM has high
    // latency.
  }

  public void close() {
    overlay.getStyle().set("display", "none");
    panel.getStyle().set("display", "none");
    resultsGrid.deselectAll();
    selectionCountLabel.setText("");
    connectButton.setEnabled(false);
    // Clear the flag so the next open() does not fire the grid's
    // automatic first-page fetch against a stale default instance.
    searchInitiated = false;
    // Reset access filter toggle to default ("All datasets")
    accessFilter = null;
    updateToggleState();
    // Hide credential banner
    credentialBanner.removeAll();
    credentialBanner.getStyle().set("display", "none");
    // Clear card errors
    cardErrors.clear();
    // Reset UI state
    loadingIndicator.getStyle().set("display", "none");
    welcomeMessage.getStyle().set("display", "flex");
    setControlsEnabled(true);
    // Clear cached results
    synchronized (cachedResults) {
      cachedResults.clear();
    }
    // Unbind UI context
    uiHandle.unbind();
  }

  public Registration addDatasetsConnectedListener(
      ComponentEventListener<DatasetsConnectedEvent> listener) {
    return addListener(DatasetsConnectedEvent.class, listener);
  }

  // ── Internal build ──────────────────────────────────────────────────

  private Div buildSidebarBody() {
    // .flex-vertical gives the body its column direction;
    // .height-full makes it fill the panel; .cds-body adds box-sizing:border-box.
    var body = new Div();
    body.addClassNames("flex-vertical", "height-full", "cds-body");

    // Header — padding and border-bottom live in .cds-header;
    // flex direction, alignment, gap, and flex-shrink use Lumo utilities.
    var header = new Div();
    header.addClassNames(
        "flex-horizontal", "items-center", "gap-s", "flex-shrink-0", "cds-header");

    var sidebarTitle = new Span("Connect Datasets");
    sidebarTitle.addClassNames("heading-3", "flex-grow");

    var closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    closeButton.setTooltipText("Close");
    closeButton.addClickListener(e -> close());

    header.add(sidebarTitle, closeButton);
    body.add(header);

    // Scrollable content area (search form + results container).
    // .flex-vertical sets the column direction, .flex-grow fills available
    // space, .scroll-vertical gives it overflow-y:scroll, and .cds-content
    // sets the padding and min-height:0 critical for flex-column children.
    var content = new Div();
    content.addClassNames("flex-vertical", "flex-grow", "scroll-vertical", "cds-content");

    // Search row: instance-selector + search field.
    // .items-end aligns bottoms so the shorter selector button aligns with
    // the taller text field. min-width:0 in .cds-search-row prevents
    // the search field from overflowing a narrow panel (classic flex trap).
    var searchRow = new Div();
    searchRow.addClassNames(
        "flex-horizontal", "items-end", "gap-xs", "mb-s", "cds-search-row");

    instanceSelector.setWidth("200px");
    searchRow.add(instanceSelector, searchField);

    var searchButtonBar = new Div();
    searchButtonBar.addClassNames("flex-horizontal", "gap-xs");

    searchButton = new Button("Search", VaadinIcon.SEARCH.create());
    searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    searchButton.addClickListener(e -> refreshSearchResults());
    
    var clearButton = new Button("Clear");
    clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    clearButton.addClickListener(e -> {
      searchField.clear();
      resultsGrid.deselectAll();
      refreshSearchResults();
    });

    searchButtonBar.add(searchButton, clearButton);

    // Access filter toggle (segmented button pair) — placed between
    // the search row and the results container.
    var toggleBar = new Div();
    toggleBar.addClassNames("flex-horizontal", "gap-00", "mb-s", "cds-toggle-bar");
    toggleBar.add(allDatasetsButton, restrictedOnlyButton);

    // Credential banner — shown when the user has no valid provider
    // connection for the selected instance.
    credentialBanner.addClassNames("cds-credential-banner");
    credentialBanner.getStyle().set("display", "none");

    content.add(searchRow, searchButtonBar, toggleBar, credentialBanner);

    // Results container was already configured with .flex-vertical +
    // .flex-grow + .cds-results in the constructor (see resultsContainer
    // setup). No inline overrides needed here — .cds-results sets
    // position:relative + min-height:200px to keep at least one card row
    // visible even before data has loaded.
    content.add(resultsContainer);

    body.add(content);

    // Footer — padding + border-top live in .cds-footer;
    // flex direction and flex-shrink use utilities.
    var footer = new Div();
    footer.addClassNames("flex-vertical", "flex-shrink-0", "cds-footer");
    
    // Experiment picker (optional association — AC9)
    // .mb-m replaces the inline margin-bottom with a Lumo utility.
    var experimentSection = new Div();
    experimentSection.addClassNames("flex-vertical", "gap-01", "mb-m");
    experimentSection.add(experimentSelector);
    var experimentHelp = new Span(
        "Optionally link the connected dataset(s) to a specific experiment.");
    experimentHelp.addClassNames("extra-small-body-text", "color-secondary");
    experimentSection.add(experimentHelp);
    footer.add(experimentSection);

    // Connect button row
    var buttonRow = new Div();
    buttonRow.addClassNames("flex-horizontal", "items-center", "gap-03");
    selectionCountLabel.addClassNames("extra-small-body-text", "color-secondary");
    buttonRow.add(selectionCountLabel, connectButton);
    footer.add(buttonRow);
    
    body.add(footer);

    return body;
  }

  // ── Data loading ────────────────────────────────────────────────────

  private void loadInstances() {
    availableInstances.clear();
    availableInstances.addAll(associatedDatasetService.availableInstances(SourceType.INVENIO_RDM));
    instanceSelector.setItems(availableInstances);
    if (!availableInstances.isEmpty() && instanceSelector.isEmpty()) {
      instanceSelector.setValue(availableInstances.get(0));
    }
  }

  /**
   * Loads experiments asynchronously to avoid blocking the UI thread.
   * Called from {@link #open()} so the sidebar opens instantly even
   * when the experiment query is slow.
   */
  private void loadExperimentsAsync() {
    if (context == null || context.projectId().isEmpty() || experimentInformationService == null) {
      return;
    }
    ProjectId projectId = context.projectId().orElseThrow();

    // Capture security context on the main thread for propagation to the async thread
    // This is critical because Spring Security checks happen in the async thread
    SecurityContext securityContext = SecurityContextHolder.getContext();

    CompletableFuture.runAsync(() -> {
      try {
        // Restore security context in the async thread
        SecurityContextHolder.setContext(securityContext);
        List<Experiment> experiments = experimentInformationService.findAllForProject(projectId);

        uiHandle.onUiAndPush(() -> {
          availableExperiments.clear();
          for (Experiment experiment : experiments) {
            availableExperiments.add(new ExperimentEntry(experiment.experimentId(), experiment.getName()));
          }
          experimentSelector.setItems(new ArrayList<>(availableExperiments));
        });
      } catch (Exception e) {
        log.error("Failed to load experiments for project {}: {}", projectId.value(), e.getMessage(), e);
        uiHandle.onUiAndPush(() -> {
          notificationFactory.toast("dataset.experiments.failed",
              new Object[]{}, getLocale()).open();
        });
      } finally {
        SecurityContextHolder.clearContext();
      }
    });
  }

  // ── Search ──────────────────────────────────────────────────────────

  /**
   * Triggers a re-fetch of search results by invalidating the lazy-
   * loading data provider. Called whenever the instance selector
   * changes, the user submits / clears the search field, etc.
   *
   * <p>The actual HTTP call happens inside {@link #fetchPage(Query)},
   * invoked by the grid on demand as it renders/scrolls. This method
   * simply tells the grid to re-evaluate its data; any error caught in
   * the callback is surfaced synchronously to the user afterwards.</p>
   */
  private void refreshSearchResults() {
    // Prevent duplicate concurrent searches
    if (searchInProgress) {
      return;
    }

    // Mark that user has initiated at least one search (for fetchPage guard)
    searchInitiated = true;

    // Clear any previous card errors when starting a new search
    cardErrors.clear();

    // Show loading state immediately and disable controls
    setControlsEnabled(false);
    welcomeMessage.getStyle().set("display", "none");
    loadingIndicator.getStyle().set("display", "flex");

    // Capture search parameters and user identity in main thread
    var instance = instanceSelector.getValue();
    var searchTerm = searchField.getValue();
    var currentUser = currentUserId();

    // Perform HTTP fetch asynchronously using the security-context-aware executor
    CompletableFuture.supplyAsync(() -> {
      // This runs in a thread with the security context propagated by DelegatingSecurityContextAsyncTaskExecutor
      return associatedDatasetService.searchDatasets(
          SourceType.INVENIO_RDM,
          instance.id(),
          searchTerm.trim().isEmpty() ? null : searchTerm.trim(),
          accessFilter,  // null = all, DatasetAccessFilter.RESTRICTED = restricted only
          0,          // page 0 (zero-indexed)
          100,        // pageSize - load up to 100 results in first fetch
          currentUser
      );
    }).thenAccept(result -> {
      // This runs after the async operation completes
      // Update cached results with thread-safe list
      synchronized (cachedResults) {
        cachedResults.clear();
        cachedResults.addAll(result.hits());
      }

      // Push results back to UI thread
      uiHandle.onUiAndPush(() -> {
        loadingIndicator.getStyle().set("display", "none");
        resultsGrid.getDataProvider().refreshAll();
        setControlsEnabled(true);
      });
    }).exceptionally(throwable -> {

      // Resolve a user-friendly message: if the service threw one of our
      // typed exceptions, unwrap and use its message for the toast; otherwise
      // fall back to a generic "Search failed." text. The original cause is
      // logged (with infrastructure details) by the service itself.
      final String userMessage = resolveUserMessage(throwable,
          "Search failed. Please try again in a moment.");

      // Push error back to UI thread
      uiHandle.onUiAndPush(() -> {
        loadingIndicator.getStyle().set("display", "none");
        setControlsEnabled(true);
        notificationFactory.toast("dataset.search.failed",
            new Object[]{userMessage}, getLocale()).open();
      });
      return null;
    });
  }

  /**
   * Enables or disables all search controls during async search.
   */
  private void setControlsEnabled(boolean enabled) {
    searchInProgress = !enabled;
    searchButton.setEnabled(enabled);
    searchField.setEnabled(enabled);
    instanceSelector.setEnabled(enabled);
  }

  /**
   * Lazy-loading callback wired to {@link #resultsGrid}.
   *
   * <p>Slices the {@link #cachedResults} list for virtual scrolling. This method
   * does NOT make HTTP calls - those happen asynchronously in {@link #refreshSearchResults()}.</p>
   *
   * <p>This callback is invoked by Vaadin when the grid needs a page of data - i.e. when
   * the grid first renders, or when the user scrolls.</p>
   */
  private Stream<SearchHit> fetchPage(Query<SearchHit, Void> query) {
    // Satisfy Vaadin's data provider contract: must access offset and limit
    // before returning, even on early-exit paths. Vaadin's DataCommunicator
    // verifies these are called to prevent arbitrary data returns that could
    // cause rendering bugs or memory issues.
    int offset = query.getOffset();
    int limit = query.getLimit();

    // Guard: until the user has initiated at least one search, return empty
    if (!searchInitiated) {
      return Stream.empty();
    }

    // Guard: validate offset and limit
    if (offset < 0 || limit <= 0) {
      return Stream.empty();
    }

    // Slice cachedResults for virtual scrolling
    synchronized (cachedResults) {
      return cachedResults.stream()
          .skip(offset)
          .limit(limit);
    }
  }

  private void onSelectionChanged() {
    int count = resultsGrid.getSelectedItems().size();
    if (count == 0) {
      selectionCountLabel.setText("");
      selectionCountLabel.getStyle().set("display", "none");
    } else {
      selectionCountLabel.setText(count + " selected");
      selectionCountLabel.getStyle().remove("display");
    }
    connectButton.setEnabled(count > 0);
  }

  // ── Connect confirmation ────────────────────────────────────────────

  private void connectSelectedDatasets() {
    if (context == null || context.projectId().isEmpty()) {
      return;
    }
    ProjectId projectId = context.projectId().orElseThrow();
    var instance = instanceSelector.getValue();
    var selected = new ArrayList<>(resultsGrid.getSelectedItems());
    if (selected.isEmpty()) {
      return;
    }

    // ── Pre-connect provider connection check ─────────────────────
    // If any selected dataset is access-restricted, check if user has
    // a valid provider connection
    boolean hasRestricted = selected.stream().anyMatch(hit -> !hit.isPublic());
    if (hasRestricted && !hasValidPat()) {
      // Show toast notification
      notificationFactory.toast("dataset.connected.credential.required",
          new Object[]{selected.stream().filter(hit -> !hit.isPublic()).count()},
          getLocale()).open();

      // Highlight the credential banner to draw user's attention
      credentialBanner.addClassName("cds-credential-banner--highlighted");
      return;
    }

    var experimentId = experimentSelector.isReadOnly() || experimentSelector.isEmpty()
        ? null
        : experimentSelector.getValue().id();

    uiHandle.bind(UI.getCurrent());

    // 1. Build request list and mark rows as PENDING (no need — just build requests)
    Map<String, String> requestIdToHandle = new HashMap<>();
    List<AssociatedDatasetService.ConnectDatasetRequest> requests = new ArrayList<>();
    for (SearchHit hit : selected) {
      String reqId = "req-" + UUID.randomUUID();
      requestIdToHandle.put(reqId, hit.externalHandleValue());
      requests.add(new AssociatedDatasetService.ConnectDatasetRequest(
          reqId, projectId, SourceType.INVENIO_RDM, instance.id(),
          hit.externalHandleValue(), experimentId, currentUserId()));
    }

    // 2. UI: enter "connecting" state immediately
    loadingMessage.setText("Connecting datasets...");
    loadingIndicator.getStyle().set("display", "flex");
    setControlsEnabled(false);
    resultsGrid.getDataProvider().refreshAll();

    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);
    final AtomicInteger alreadyConnectedCount = new AtomicInteger(0);
    final AtomicInteger credentialErrorCount = new AtomicInteger(0);
    final AtomicInteger accessLinkErrorCount = new AtomicInteger(0);
    final AtomicInteger completedCount = new AtomicInteger(0);
    final int total = requests.size();

    // 3. Fire-and-subscribe — callbacks run on boundedElastic worker threads.
    // Count completed responses via AtomicInteger (the 3-arg subscribe's
    // onComplete callback is not reliable with subscribeOn + onErrorResume).
    associatedDatasetService.connectDatasets(requests)
        .doOnError(error -> log.error("Unexpected error in connect stream", error))
        .subscribe(response -> {
          try {
            // Find the external handle for this response
            String externalHandle = requestIdToHandle.get(response.requestId());

            // Update counters and track per-card errors
            if (response.associatedDatasetId() != null) {
              successCount.incrementAndGet();
              // Clear any previous error for this card
              cardErrors.remove(externalHandle);
            } else if (response.error()
                == life.qbic.projectmanagement.application.associated_dataset.ConnectDatasetError.ALREADY_CONNECTED) {
              alreadyConnectedCount.incrementAndGet();
              cardErrors.remove(externalHandle);
            } else if (response.error()
                == life.qbic.projectmanagement.application.associated_dataset.ConnectDatasetError.CREDENTIAL_REQUIRED) {
              credentialErrorCount.incrementAndGet();
              failureCount.incrementAndGet();
              cardErrors.put(externalHandle, "Provider connection required");
            } else if (response.error()
                == life.qbic.projectmanagement.application.associated_dataset.ConnectDatasetError.ACCESS_LINK_CREATION_FAILED) {
              accessLinkErrorCount.incrementAndGet();
              failureCount.incrementAndGet();
              cardErrors.put(externalHandle, "You don't have permission to create a shareable link for this dataset");
            } else {
              failureCount.incrementAndGet();
              cardErrors.put(externalHandle, "Connection failed");
            }

            // When all responses are in, show results (keep sidebar open)
            if (completedCount.incrementAndGet() == total) {
              onBatchFinished(successCount.get(), failureCount.get(),
                  alreadyConnectedCount.get(), credentialErrorCount.get(),
                  accessLinkErrorCount.get());
            }
          } catch (Exception e) {
            // Never let an exception escape — would freeze the UI
            log.error("Exception in connect response handler", e);
            failureCount.incrementAndGet();
            if (completedCount.incrementAndGet() == total) {
              onBatchFinished(successCount.get(), failureCount.get(),
                  alreadyConnectedCount.get(), credentialErrorCount.get(),
                  accessLinkErrorCount.get());
            }
          }
        });
  }

  /**
   * Hides the spinner, refreshes the grid to show inline errors,
   * fires appropriate toasts, and dispatches DatasetsConnectedEvent
   * to refresh the parent list.
   * Called on the Reactor worker thread — UI work goes through UiHandle.
   */
  private void onBatchFinished(int successCount, int failureCount,
      int alreadyConnectedCount, int credentialErrorCount,
      int accessLinkErrorCount) {
    uiHandle.onUiAndPush(() -> {
      try {
        // Hide spinner and restore controls
        loadingIndicator.getStyle().set("display", "none");
        loadingMessage.setText("Searching for datasets...");
        setControlsEnabled(true);

        // If every connection attempt succeeded, close the sidebar.
        // Otherwise keep it open so inline errors on failed cards
        // (credential/access-link failures) stay visible.
        if (failureCount == 0) {
          close();
        } else {
          // Refresh the grid to show inline errors on failed cards
          resultsGrid.getDataProvider().refreshAll();
        }

        // Show toast with results
        if (successCount > 0) {
          notificationFactory.toast("dataset.connected.success",
              new Object[]{successCount}, getLocale()).open();
          fireEvent(new DatasetsConnectedEvent(this));
        }
        if (alreadyConnectedCount > 0) {
          notificationFactory.toast("dataset.connected.already",
              new Object[]{alreadyConnectedCount}, getLocale()).open();
        }
        if (credentialErrorCount > 0) {
          notificationFactory.toast("dataset.connected.credential.required",
              new Object[]{credentialErrorCount}, getLocale()).open();
        }
        if (accessLinkErrorCount > 0) {
          notificationFactory.toast("dataset.connected.access.link.failed",
              new Object[]{accessLinkErrorCount}, getLocale()).open();
        }
        // Generic failure toast for other failures (not credential or access link)
        int otherFailures = failureCount - credentialErrorCount - accessLinkErrorCount;
        if (otherFailures > 0) {
          notificationFactory.toast("dataset.connected.failure",
              new Object[]{otherFailures}, getLocale()).open();
        }
      } catch (Exception e) {
        log.error("Exception in onBatchFinished", e);
      }
    });
  }

  // ── Card-style row ─────────────────────────────────────────────────

  private Component buildSearchResultCard(SearchHit hit) {
    // .border + .p-m + .mb-s + .clickable cover padding, spacing, and cursor;
    // .cds-card adds the rounded border.
    var card = new Div();
    card.addClassNames("border", "p-m", "mb-s", "clickable", "cds-card");
    // Clicking the card toggles selection (row click listener is attached
    // to the grid). .clickable makes the affordance explicit.

    // Top row: provider tag + access badge + date.
    // .flex-horizontal + .items-center + .gap-02 from Lumo utilities;
    // .mb-xs replaces the inline margin-bottom with a Lumo utility.
    var topRow = new Div();
    topRow.addClassNames("flex-horizontal", "items-center", "gap-02", "mb-xs");

    // Provider tag — styled via centralized factory so this view and the
    // connected-resources list share the same color scheme.
    String provider = hit.resourceProvider();
    if (provider != null && !provider.isBlank()) {
      topRow.add(DataSetTagFactory.create(TagType.PROVIDER, provider));
    }

    // Access badge — styled via centralized factory
    topRow.add(DataSetTagFactory.create(
        TagType.ACCESS_TYPE, hit.isPublic()));

    // Date — pushed to the trailing edge of the flex row via .ml-auto
    // (Lumo margin utility replaces inline margin-left:auto).
    var dateSpan = new Span(hit.publicationDate().format(
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
    dateSpan.addClassNames("extra-small-body-text", "color-secondary", "ml-auto");
    topRow.add(dateSpan);

    card.add(topRow);

    // Title — clamped to 2 visible lines with a hover tooltip for the
    // full title. .clamp-2-line + .normal-body-text live in all.css;
    // .cds-card-title + .mb-xs handle weight and bottom margin.
    var title = clampableTitle(hit.title(), 2);
    title.addClassNames("cds-card-title", "mb-xs");
    card.add(title);

    // PID rendered as a clickable link — opens the record in a new tab so
    // users can verify it before connecting. The "PID:" label is implicit
    // and removed to reduce visual noise.
    String pidHref = hit.pid().startsWith("http")
        ? hit.pid() : "https://doi.org/" + hit.pid();
    var pidLink = new Anchor(pidHref, hit.pid());
    pidLink.setTarget(AnchorTarget.BLANK);
    pidLink.addClassName("extra-small-body-text");

    var metaRow = new Div();
    metaRow.addClassNames("flex-horizontal", "gap-04", "items-center");
    metaRow.add(pidLink);
    card.add(metaRow);

    // Inline error message (shown when access link creation fails)
    String errorMessage = cardErrors.get(hit.externalHandleValue());
    if (errorMessage != null) {
      var errorDiv = new Div();
      errorDiv.addClassName("cds-card-error");
      var errorIcon = VaadinIcon.WARNING.create();
      errorIcon.addClassName("cds-card-error-icon");
      var errorText = new Span(errorMessage);
      errorText.addClassName("cds-card-error-text");
      errorDiv.add(errorIcon, errorText);
      card.add(errorDiv);
    }

    return card;
  }

  // ── Helpers ─────────────────────────────────────────────────────────

  /**
   * Checks if the current user has a valid provider connection for the selected instance.
   *
   * @return true if user has a valid provider connection, false otherwise
   */
  private boolean hasValidPat() {
    var instance = instanceSelector.getValue();
    if (instance == null) {
      return false;
    }
    try {
      CredentialStatus status = externalCredentialService.credentialStatusForInstance(
          currentUserId(), instance.id());
      return status == CredentialStatus.VALID;
    } catch (Exception e) {
      log.error("Failed to check credential status", e);
      return false;
    }
  }

  /**
   * Updates the visual state of the segmented button pair to reflect
   * the current {@link #accessFilter} value (a {@link DatasetAccessFilter},
   * or null for all datasets). The active button gets the
   * {@code cds-segmented-btn--active} class; the inactive one loses it.
   */
  private void updateToggleState() {
    if (accessFilter == null) {
      allDatasetsButton.addClassName("cds-segmented-btn--active");
      restrictedOnlyButton.removeClassName("cds-segmented-btn--active");
    } else {
      allDatasetsButton.removeClassName("cds-segmented-btn--active");
      restrictedOnlyButton.addClassName("cds-segmented-btn--active");
    }
  }

  /**
   * Updates the credential banner visibility and content based on
   * the user's provider connection status for the selected instance.
   *
   * <p>The banner is shown when the user has no valid provider connection
   * for the selected instance (either NOT_CONFIGURED or INVALIDATED),
   * regardless of the access filter setting.</p>
   *
   * <p>The banner is informational (non-blocking) — the search
   * proceeds regardless. Without a connection, connecting restricted
   * datasets will fail with inline errors on the cards.</p>
   */
  private void updateCredentialBanner() {
    var instance = instanceSelector.getValue();
    if (instance == null) {
      credentialBanner.getStyle().set("display", "none");
      return;
    }

    CredentialStatus status;
    try {
      status = externalCredentialService.credentialStatusForInstance(
          currentUserId(), instance.id());
    } catch (Exception e) {
      log.error("Failed to check credential status for instance %s"
          .formatted(instance.id()), e);
      credentialBanner.getStyle().set("display", "none");
      return;
    }

    credentialBanner.removeAll();

    if (status == CredentialStatus.NOT_CONFIGURED) {
      var icon = VaadinIcon.INFO_CIRCLE.create();
      icon.addClassName("cds-credential-banner-icon");
      var text = new Span(
          "No connection configured for " + instance.displayName()
          + ". Configure a connection to connect restricted datasets.");
      text.addClassName("cds-credential-banner-text");
      credentialBanner.add(icon, text, configureConnectionButton);
      credentialBanner.addClassName("cds-credential-banner--info");
      credentialBanner.removeClassName("cds-credential-banner--warning");
      credentialBanner.getStyle().set("display", "flex");
    } else if (status == CredentialStatus.INVALIDATED) {
      var icon = VaadinIcon.WARNING.create();
      icon.addClassName("cds-credential-banner-icon");
      var text = new Span(
          "Your connection to " + instance.displayName()
          + " was rejected. Reconnect to connect restricted datasets.");
      text.addClassName("cds-credential-banner-text");
      credentialBanner.add(icon, text, configureConnectionButton);
      credentialBanner.removeClassName("cds-credential-banner--info");
      credentialBanner.addClassName("cds-credential-banner--warning");
      credentialBanner.getStyle().set("display", "flex");
    } else {
      // VALID — no banner needed
      credentialBanner.getStyle().set("display", "none");
    }
  }

  /**
   * Resolves a user-friendly message from an async throwable. Walks the
   * cause chain in case of {@code CompletionException} / ExecutionException
   * wrapping, and returns the service-specific user message when found;
   * otherwise falls back to the given {@code defaultMessage}.
   */
  private String resolveUserMessage(Throwable throwable, String defaultMessage) {
    Throwable cause = throwable;
    while (cause instanceof CompletionException || cause instanceof ExecutionException) {
      cause = cause.getCause();
      if (cause == null) {
        return defaultMessage;
      }
    }
    if (cause instanceof AssociatedDatasetServiceException serviceException) {
      return serviceException.userMessage();
    }
    return defaultMessage;
  }

  private String currentUserId() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    return authenticationToUserIdTranslator.translateToUserId(authentication).orElseThrow(() -> {
      log.error("Could not translate authentication to user ID");
      return new IllegalStateException("Could not translate authentication to user ID");
    });
  }

  /** A lightweight record mapping an ExperimentId to its display name. */
  record ExperimentEntry(ExperimentId id, String label) {
    @Override
    public @NonNull String toString() {
      return label;
    }
  }

  // ── Custom event ────────────────────────────────────────────────────

  /**
   * Fired when one or more datasets have been successfully connected to
   * the project. The parent view refreshes its connected-resources grid
   * in response.
   */
  public static class DatasetsConnectedEvent
      extends com.vaadin.flow.component.ComponentEvent<ConnectDatasetSidebar> {

    @Serial
    private static final long serialVersionUID = 1L;

    public DatasetsConnectedEvent(ConnectDatasetSidebar source) {
      super(source, false);
    }
  }
}
