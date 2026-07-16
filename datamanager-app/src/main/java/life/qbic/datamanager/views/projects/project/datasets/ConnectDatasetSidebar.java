package life.qbic.datamanager.views.projects.project.datasets;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.SearchHit;
import life.qbic.projectmanagement.application.associated_dataset.SearchResult;
import life.qbic.projectmanagement.application.associated_dataset.SourceInstanceDescriptor;
import life.qbic.projectmanagement.application.associated_dataset.ConnectDatasetError;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import java.util.concurrent.CompletableFuture;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.associated_dataset.SourceType;
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

  @Serial
  private static final long serialVersionUID = 1L;

  private final AssociatedDatasetService associatedDatasetService;
  private final ExperimentInformationService experimentInformationService;

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
  private final Div welcomeMessage;

  /**
   * Buffer for errors caught inside the lazy-loading callback. The
   * callback cannot show a Notification directly because it fires
   * during Grid rendering; instead it stores the message here and
   * {@link #refreshSearchResults()} surfaces it afterwards.
   */
  private volatile String lastSearchError;

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
      Object userPermissions) {
    this.associatedDatasetService = associatedDatasetService;
    this.experimentInformationService = experimentInformationService;

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
    searchField.getStyle().set("flex-grow", "1");
    searchField.addKeyDownListener(Key.ENTER, e -> refreshSearchResults());

    resultsGrid = new Grid<>();
    resultsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_ROW_BORDERS);
    resultsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
    resultsGrid.setWidthFull();
    resultsGrid.setHeight("400px"); // Fixed height to enable virtual scrolling
    resultsGrid.addComponentColumn(this::buildSearchResultCard).setFlexGrow(1);
    resultsGrid.addSelectionListener(e -> onSelectionChanged());

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
    // Indeterminate spinner + clear messaging, shown during searches
    var spinner = new Span();
    spinner.getElement().setProperty("innerHTML", "&#8987;"); // hourglass symbol
    spinner.getStyle().set("font-size", "var(--lumo-font-size-xxxl)");
    spinner.getStyle().set("animation", "spin 1s linear infinite");
    
    var loadingLabel = new Span("Searching for datasets...");
    loadingLabel.addClassName("normal-body-text");
    loadingLabel.getStyle().set("font-weight", "500");
    
    var loadingHint = new Span("This may take a few seconds");
    loadingHint.addClassName("small-body-text");
    loadingHint.getStyle().set("color", "var(--lumo-tertiary-text-color)");

    loadingIndicator = new Div();
    loadingIndicator.addClassNames("flex-vertical", "items-center", "gap-02");
    loadingIndicator.getStyle().set("position", "absolute");
    loadingIndicator.getStyle().set("top", "0");
    loadingIndicator.getStyle().set("left", "0");
    loadingIndicator.getStyle().set("width", "100%");
    loadingIndicator.getStyle().set("height", "100%");
    loadingIndicator.getStyle().set("justify-content", "center");
    loadingIndicator.getStyle().set("background-color", "var(--lumo-base-color)");
    loadingIndicator.getStyle().set("z-index", "2");
    loadingIndicator.add(spinner, loadingLabel, loadingHint);

    // ── Welcome message (shown before first search) ──────────────────
    welcomeMessage = new Div();
    welcomeMessage.addClassNames("flex-vertical", "items-center", "gap-02");
    welcomeMessage.getStyle().set("position", "absolute");
    welcomeMessage.getStyle().set("top", "0");
    welcomeMessage.getStyle().set("left", "0");
    welcomeMessage.getStyle().set("width", "100%");
    welcomeMessage.getStyle().set("height", "100%");
    welcomeMessage.getStyle().set("justify-content", "center");
    welcomeMessage.getStyle().set("background-color", "var(--lumo-base-color)");
    welcomeMessage.getStyle().set("color", "var(--lumo-secondary-text-color)");
    welcomeMessage.getStyle().set("z-index", "1");
    welcomeMessage.getStyle().set("cursor", "default");
    
    var welcomeIcon = VaadinIcon.SEARCH.create();
    welcomeIcon.getStyle().set("font-size", "var(--lumo-font-size-xxxl)");
    welcomeIcon.getStyle().set("color", "var(--lumo-tertiary-text-color)");
    
    var welcomeTitle = new Span("Search for datasets");
    welcomeTitle.addClassName("heading-4");
    welcomeTitle.getStyle().set("margin-top", "var(--lumo-space-s)");
    
    var welcomeSubtitle = new Span(
        "Use the search field above to find open datasets you can connect to this project.");
    welcomeSubtitle.addClassName("body-text");
    welcomeSubtitle.getStyle().set("text-align", "center");
    welcomeSubtitle.getStyle().set("padding", "0 var(--lumo-space-l)");
    welcomeSubtitle.getStyle().set("color", "var(--lumo-tertiary-text-color)");
    
    welcomeMessage.add(welcomeIcon, welcomeTitle, welcomeSubtitle);

    resultsContainer = new Div();
    resultsContainer.addClassNames("flex-vertical");
    resultsContainer.getStyle().set("position", "relative");
    // Make this container fill remaining space in the flex column
    resultsContainer.getStyle().set("flex-grow", "1");
    resultsContainer.getStyle().set("min-height", "0"); // Critical for flex layout
    resultsContainer.add(loadingIndicator, welcomeMessage, resultsGrid);

    experimentSelector = new ComboBox<>();
    experimentSelector.setLabel("Link to experiment (optional)");
    experimentSelector.setPlaceholder("No experiment selected");
    experimentSelector.setItems(availableExperiments);
    experimentSelector.setItemLabelGenerator(ExperimentEntry::label);
    experimentSelector.setClearButtonVisible(true);
    experimentSelector.setWidthFull();

    selectionCountLabel = new Span();
    selectionCountLabel.addClassName("normal-body-text");
    selectionCountLabel.addClassName("color-secondary");

    connectButton = new Button("Connect Selected", VaadinIcon.PLUS_CIRCLE.create());
    connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectButton.setEnabled(false);
    connectButton.addClickListener(e -> connectSelectedDatasets());

    // ── Overlay (semi-transparent backdrop) ──────────────────────────
    overlay = new Div();
    overlay.getStyle().set("position", "fixed");
    overlay.getStyle().set("top", "0");
    overlay.getStyle().set("left", "0");
    overlay.getStyle().set("width", "100%");
    overlay.getStyle().set("height", "100%");
    overlay.getStyle().set("background-color", "rgba(0,0,0,0.3)");
    overlay.getStyle().set("z-index", "999");
    overlay.getStyle().set("display", "none");
    overlay.addClickListener(e -> close());
    add(overlay);

    // ── Panel ─────────────────────────────────────────────────────────
    panel = new Div();
    panel.getStyle().set("position", "fixed");
    panel.getStyle().set("top", "0");
    panel.getStyle().set("right", "0");
    panel.getStyle().set("width", "640px");
    panel.getStyle().set("height", "100%");
    panel.getStyle().set("background-color", "var(--lumo-base-color)");
    panel.getStyle().set("box-shadow", "-4px 0 24px rgba(0,0,0,0.12)");
    panel.getStyle().set("z-index", "1000");
    panel.getStyle().set("box-sizing", "border-box");
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
    // Reset UI state
    loadingIndicator.getStyle().set("display", "none");
    welcomeMessage.getStyle().set("display", "flex");
    setControlsEnabled(true);
    // Clear cached results
    synchronized (cachedResults) {
      cachedResults.clear();
    }
  }

  public Registration addDatasetsConnectedListener(
      ComponentEventListener<DatasetsConnectedEvent> listener) {
    return addListener(DatasetsConnectedEvent.class, listener);
  }

  // ── Internal build ──────────────────────────────────────────────────

  private Div buildSidebarBody() {
    var body = new Div();
    body.getStyle().set("height", "100%");
    body.getStyle().set("box-sizing", "border-box");
    body.getStyle().set("display", "flex");
    body.getStyle().set("flex-direction", "column");

    // Header
    var header = new Div();
    header.addClassNames("flex-horizontal", "items-center");
    header.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
    header.getStyle().set("flex-shrink", "0");
    header.getStyle().set("gap", "var(--lumo-space-s)");

    var sidebarTitle = new Span("Connect Datasets");
    sidebarTitle.addClassName("heading-3");
    sidebarTitle.getStyle().set("flex-grow", "1");

    var closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    closeButton.setTooltipText("Close");
    closeButton.addClickListener(e -> close());

    header.add(sidebarTitle, closeButton);
    body.add(header);

    // Content area - handles search form and results
    var content = new Div();
    content.getStyle().set("flex-grow", "1");
    content.getStyle().set("padding", "var(--lumo-space-l)");
    content.getStyle().set("display", "flex");
    content.getStyle().set("flex-direction", "column");
    content.getStyle().set("min-height", "0");
    content.getStyle().set("overflow-y", "auto"); // Make content scrollable

    var searchRow = new Div();
    searchRow.getStyle().set("display", "flex");
    searchRow.getStyle().set("gap", "var(--lumo-space-xs)");
    searchRow.getStyle().set("align-items", "flex-end");
    searchRow.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    searchRow.getStyle().set("min-width", "0");

    instanceSelector.setWidth("200px");
    searchRow.add(instanceSelector, searchField);

    var searchButtonBar = new Div();
    searchButtonBar.getStyle().set("display", "flex");
    searchButtonBar.getStyle().set("gap", "var(--lumo-space-xs)");

    searchButton = new Button("Search", VaadinIcon.SEARCH.create());
    searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    searchButton.addClickListener(e -> refreshSearchResults());
    
    var clearButton = new Button("Clear");
    clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    clearButton.addClickListener(e -> {
      searchField.clear();
      refreshSearchResults();
    });

    searchButtonBar.add(searchButton, clearButton);
    content.add(searchRow, searchButtonBar);
    
    // Results container - takes remaining space in content
    resultsContainer.getStyle().set("flex-grow", "1");
    resultsContainer.getStyle().set("min-height", "200px"); // Minimum height for grid
    content.add(resultsContainer);

    body.add(content);

    // Footer with experiment selector and connect button
    var footer = new Div();
    footer.addClassNames("flex-vertical");
    footer.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    footer.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
    footer.getStyle().set("flex-shrink", "0");
    
    // Experiment picker (optional association — AC9)
    var experimentSection = new Div();
    experimentSection.addClassNames("flex-vertical", "gap-01");
    experimentSection.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    experimentSection.add(experimentSelector);
    var experimentHelp = new Span(
        "Optionally link the connected dataset(s) to a specific experiment.");
    experimentHelp.addClassName("extra-small-body-text");
    experimentHelp.addClassName("color-secondary");
    experimentSection.add(experimentHelp);
    footer.add(experimentSection);

    // Connect button row
    var buttonRow = new Div();
    buttonRow.addClassNames("flex-horizontal", "items-center", "gap-03");
    selectionCountLabel.addClassName("extra-small-body-text");
    selectionCountLabel.addClassName("color-secondary");
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
    if (context == null || context.projectId().isEmpty()) {
      return;
    }
    ProjectId projectId = context.projectId().orElseThrow();
    // Fetch in background thread, then update UI on the main thread
    CompletableFuture.supplyAsync(
            () -> experimentInformationService.findAllForProject(projectId))
        .thenAccept(experiments -> {
          // Schedule UI update on Vaadin's UI access thread
          UI ui = UI.getCurrent();
          if (ui != null) {
            ui.access(() -> {
              availableExperiments.clear();
              for (Experiment exp : experiments) {
                availableExperiments.add(new ExperimentEntry(exp.experimentId(), exp.getName()));
              }
              experimentSelector.setItems(availableExperiments);
            });
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
    lastSearchError = null;

    // Show loading state immediately and disable controls
    setControlsEnabled(false);
    welcomeMessage.getStyle().set("display", "none");
    loadingIndicator.getStyle().set("display", "flex");

    // Capture UI reference before async operation
    var ui = UI.getCurrent();

    // Capture search parameters and user identity
    var instance = instanceSelector.getValue();
    var searchTerm = searchField.getValue();
    var currentUser = currentUserId();

    // Perform HTTP fetch in background thread
    new Thread(() -> {
      try {
        // Perform the actual search with all required parameters
        var result = associatedDatasetService.searchDatasets(
            SourceType.INVENIO_RDM,
            instance.id(),
            searchTerm.trim().isEmpty() ? null : searchTerm.trim(),
            0,          // page 0 (zero-indexed)
            100,        // pageSize - load up to 100 results in first fetch
            currentUser
        );

        // Update cached results with thread-safe list
        synchronized (cachedResults) {
          cachedResults.clear();
          cachedResults.addAll(result.hits());
        }

        // Push results back to UI thread
        if (ui != null) {
          ui.access(() -> {
            loadingIndicator.getStyle().set("display", "none");
            resultsGrid.getDataProvider().refreshAll();
            setControlsEnabled(true);
          });
        }
      } catch (Exception e) {
        // Store error for display
        lastSearchError = e.getMessage();

        // Push error back to UI thread
        if (ui != null) {
          ui.access(() -> {
            loadingIndicator.getStyle().set("display", "none");
            setControlsEnabled(true);
            showErrorNotification("Search failed: " + lastSearchError);
          });
        }
      }
    }).start();
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
    selectionCountLabel.setText(count == 0 ? "" : count + " selected");
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
    var experimentId = experimentSelector.isReadOnly() || experimentSelector.isEmpty()
        ? null
        : experimentSelector.getValue().id();

    int successCount = 0;
    int failureCount = 0;

    String actingUser = currentUserId();
    for (SearchHit hit : selected) {
      var result = associatedDatasetService.connectDataset(
          projectId,
          SourceType.INVENIO_RDM,
          instance.id(),
          hit.externalHandleValue(),
          java.util.Optional.ofNullable(experimentId),
          actingUser);
      if (result.isValue()) {
        successCount++;
      } else {
        failureCount++;
      }
    }

    resultsGrid.deselectAll();
    onSelectionChanged();
    close();

    if (successCount > 0) {
      showSuccessNotification(
          "%d dataset(s) connected to this project.".formatted(successCount));
      fireEvent(new DatasetsConnectedEvent(this));
    }
    if (failureCount > 0) {
      showErrorNotification(
          "%d dataset(s) could not be connected.".formatted(failureCount));
    }
  }

  // ── Card-style row ──────────────────────────────────────────────────

  private Component buildSearchResultCard(SearchHit hit) {
    var card = new Div();
    card.addClassName("border");
    card.getStyle().set("padding", "var(--lumo-space-m)");
    card.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    card.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

    // Top row: access badge + provider + date
    var topRow = new Div();
    topRow.addClassNames("flex-horizontal", "items-center", "gap-02");
    topRow.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

    Tag accessBadge = hit.accessLevel() == AccessLevel.PUBLIC
        ? new Tag("Public") : new Tag("Restricted");
    accessBadge.setTagColor(hit.accessLevel() == AccessLevel.PUBLIC
        ? TagColor.SUCCESS : TagColor.WARNING);
    topRow.add(accessBadge);

    var providerTag = new Tag(hit.resourceProvider());
    providerTag.setTagColor(TagColor.PRIMARY);
    topRow.add(providerTag);

    var dateSpan = new Span(hit.publicationDate().format(
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
    dateSpan.addClassName("extra-small-body-text");
    dateSpan.addClassName("color-secondary");
    dateSpan.getStyle().set("margin-left", "auto");
    topRow.add(dateSpan);

    card.add(topRow);

    // Title
    var title = new Span(hit.title());
    title.addClassName("normal-body-text");
    title.getStyle().set("font-weight", "600");
    title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
    title.getStyle().set("display", "block");
    card.add(title);

    // Creator + DOI row
    var metaRow = new Div();
    metaRow.addClassNames("flex-horizontal", "gap-04", "items-center");
    metaRow.add(new Span("PID: " + hit.pid()));
    card.add(metaRow);

    return card;
  }

  // ── Helpers ─────────────────────────────────────────────────────────

  private String currentUserId() {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof QbicUserDetails ud) {
      return ud.getUserId();
    }
    throw new IllegalStateException("Unable to resolve current user identity");
  }

  private void showSuccessNotification(String message) {
    var n = new Notification(message, 3500);
    n.addClassName("success-toast");
    n.setPosition(Notification.Position.BOTTOM_END);
    n.open();
  }

  private void showErrorNotification(String message) {
    var n = new Notification(message, 4500);
    n.addClassName("error-toast");
    n.setPosition(Notification.Position.BOTTOM_END);
    n.open();
  }

  /** A lightweight record mapping an ExperimentId to its display name. */
  record ExperimentEntry(ExperimentId id, String label) {
    @Override
    public String toString() {
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
