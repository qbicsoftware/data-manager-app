package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.ButtonFactory;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.DialogSection;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.general.icon.IconFactory;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.Section.SectionBuilder;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;

/**
 * <b>Associated Datasets UI Prototype</b>
 *
 * <p>A pure UI prototype for the "Connect Associated Datasets" feature stories. Demonstrates all
 * user-facing interactions without any backend integration:</p>
 *
 * <ul>
 *   <li>Story 01 / 05 – Search and connect open / restricted datasets</li>
 *   <li>Story 02 / 06 – View connected datasets (public and restricted)</li>
 *   <li>Story 03 / 07 – Remove a connected dataset</li>
 *   <li>Story 04 / 08 – Sync a connected dataset (single and all)</li>
 * </ul>
 *
 * <p>Only available when the {@code development} Spring profile is active.</p>
 *
 * @since 1.11.0
 */
@Profile("development")
@Route("demo/associated-datasets")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
public class AssociatedDatasetsDemo extends Div {

  // ── sample stub records ─────────────────────────────────────────────────────

  record ConnectedDataset(
      String doi,
      String title,
      String repository,
      String type,          // "public" | "restricted"
      String connectedBy,
      LocalDate connectedOn,
      String version,
      String accessUrl
  ) {}

  record SearchResult(
      String doi,
      String title,
      String creators,
      String repository,
      String type,          // "public" | "restricted"
      String version
  ) {}

  // ── stub data (mimics InvenioRDM search results) ─────────────────────────────

  private static final List<SearchResult> STUB_PUBLIC_RESULTS = List.of(
      new SearchResult("10.5281/zenodo.10371779", "Multi-omics raw data supplement", "Schmidt J., Müller L.", "Zenodo", "public", "v2"),
      new SearchResult("10.5281/zenodo.8201342", "Proteomics SILAC dataset", "Patel R., Kim S.", "Zenodo", "public", "v1"),
      new SearchResult("10.15496/publikation-94781", "TMT-based phosphoproteomics", "Torres A., Chen W.", "FDAT", "public", "v1"),
      new SearchResult("10.5281/zenodo.7654321", "NGS QC reports and figures", "Andersen L.", "Zenodo", "public", "v3"),
      new SearchResult("10.15496/publikation-88812", "Sample preparation protocol", "Kowalski S., Garcia M.", "FDAT", "public", "v2")
  );

  private static final List<SearchResult> STUB_RESTRICTED_RESULTS = List.of(
      new SearchResult("10.5281/zenodo.99987654", "Unpublished immunopeptidomics run A", "Schmidt J.", "Zenodo", "restricted", "v1"),
      new SearchResult("10.15496/publikation-11111", "Restricted clinical proteomics panel", "Müller L., Torres A.", "FDAT", "restricted", "v1"),
      new SearchResult("10.5281/zenodo.88881234", "Internal biobank sample metadata", "Kim S.", "Zenodo", "restricted", "v2")
  );

  // connected datasets – mutable list so add/remove works during a demo session
  private final List<ConnectedDataset> connectedDatasets = new ArrayList<>(List.of(
      new ConnectedDataset("10.5281/zenodo.10371779", "Multi-omics raw data supplement",
          "Zenodo", "public", "j.smith@uni-tuebingen.de", LocalDate.of(2026, 5, 10), "v2",
          "https://zenodo.org/records/10371779"),
      new ConnectedDataset("10.15496/publikation-94781", "TMT-based phosphoproteomics",
          "FDAT", "public", "m.garcia@uni-tuebingen.de", LocalDate.of(2026, 6, 1), "v1",
          "https://publikationen.uni-tuebingen.de/xmlui/handle/10900/94781"),
      new ConnectedDataset("10.5281/zenodo.99987654", "Unpublished immunopeptidomics run A",
          "Zenodo", "restricted", "j.smith@uni-tuebingen.de", LocalDate.of(2026, 6, 15), "v1",
          "https://zenodo.org/records/99987654")
  ));

  // ── grid back-references so sections can refresh ─────────────────────────────
  private Grid<ConnectedDataset> publicGrid;
  private Grid<ConnectedDataset> restrictedGrid;

  // ── layout constants ─────────────────────────────────────────────────────────
  private static final String FLEX_V = "flex-vertical";
  private static final String GAP_04 = "gap-04";
  private static final String GAP_06 = "gap-06";

  private static TagColor tagColorForRepository(String repository) {
    return switch (repository) {
      case "Zenodo" -> TagColor.PRIMARY;
      case "FDAT"   -> TagColor.TEAL;
      default       -> TagColor.CONTRAST;
    };
  }

  public AssociatedDatasetsDemo() {
    addClassNames("padding-horizontal-07", "padding-vertical-04", FLEX_V, GAP_06);

    var pageTitle = new Div("Associated Datasets – UI Prototype");
    pageTitle.addClassName("heading-1");

    var intro = new Div(
        "Prototype for feature stories 01–08 of the \"Connect Associated Datasets\" feature. "
            + "All interactions are simulated in-memory – no backend is contacted.");
    intro.addClassNames("normal-body-text", "color-secondary");

    add(pageTitle, intro);
    add(buildPublicDatasetsSection());
    add(buildRestrictedDatasetsSection());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  PUBLIC DATASETS SECTION  (Stories 01, 02, 03, 04)
  // ═══════════════════════════════════════════════════════════════════════════

  private Section buildPublicDatasetsSection() {
    // action bar buttons (write-access guard simulated: always enabled in demo)
    var connectBtn = new ButtonFactory().createConfirmButton("Connect Dataset");
    var syncAllBtn = new ButtonFactory().createCancelButton("Sync All");
    syncAllBtn.setIcon(VaadinIcon.REFRESH.create());

    var actionBar = new ActionBar(connectBtn, syncAllBtn);
    actionBar.activateAllControls();

    var header = new SectionHeader(
        new SectionTitle("Public Datasets"),
        actionBar,
        new SectionNote("Open, published datasets connected to this project via InvenioRDM.")
    );

    publicGrid = buildConnectedGrid("public");

    var content = new SectionContent(publicGrid);

    var section = new SectionBuilder()
        .withHeader(header)
        .enableControls()
        .build();
    section.setContent(content);

    // wire up buttons
    connectBtn.addClickListener(e -> openSearchDialog("public"));
    syncAllBtn.addClickListener(e -> simulateSyncAll("public"));

    return section;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  RESTRICTED DATASETS SECTION  (Stories 05, 06, 07, 08)
  // ═══════════════════════════════════════════════════════════════════════════

  private Section buildRestrictedDatasetsSection() {
    var connectBtn = new ButtonFactory().createConfirmButton("Connect Dataset");
    var syncAllBtn = new ButtonFactory().createCancelButton("Sync All");
    syncAllBtn.setIcon(VaadinIcon.REFRESH.create());

    var actionBar = new ActionBar(connectBtn, syncAllBtn);
    actionBar.activateAllControls();

    var note = new SectionNote(
        "Access-restricted datasets – visible only to project members. "
            + "Requires a configured InvenioRDM access token in your account settings.");

    var header = new SectionHeader(
        new SectionTitle("Restricted Datasets"),
        actionBar,
        note
    );

    restrictedGrid = buildConnectedGrid("restricted");

    var content = new SectionContent(restrictedGrid);

    var section = new SectionBuilder()
        .withHeader(header)
        .enableControls()
        .build();
    section.setContent(content);

    connectBtn.addClickListener(e -> openSearchDialog("restricted"));
    syncAllBtn.addClickListener(e -> simulateSyncAll("restricted"));

    return section;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  CONNECTED DATASETS GRID  (Stories 02, 03, 04 / 06, 07, 08)
  // ═══════════════════════════════════════════════════════════════════════════

  private Grid<ConnectedDataset> buildConnectedGrid(String type) {
    var grid = new Grid<ConnectedDataset>();
    grid.addClassName("dataset-grid");
    grid.setAllRowsVisible(true);

    // Title column
    grid.addComponentColumn(ds -> {
      var titleSpan = new Span(ds.title());
      titleSpan.addClassName("normal-body-text");
      return titleSpan;
    }).setHeader("Title").setFlexGrow(4);

    // DOI column
    grid.addComponentColumn(ds -> {
      var anchor = new Anchor(ds.accessUrl(), ds.doi());
      anchor.setTarget("_blank");
      anchor.addClassName("small-body-text");
      return anchor;
    }).setHeader("DOI / Access Link").setFlexGrow(2);

    // Repository column
    grid.addComponentColumn(ds -> {
      var tag = new Tag(ds.repository());
      tag.setTagColor(tagColorForRepository(ds.repository()));
      return tag;
    }).setHeader("Repository").setFlexGrow(1);

    // Version column
    grid.addColumn(ConnectedDataset::version).setHeader("Version").setFlexGrow(1);

    // Connected by column
    grid.addComponentColumn(ds -> {
      var cell = new Div();
      cell.addClassNames("flex-vertical");
      var userSpan = new Span(ds.connectedBy());
      userSpan.addClassName("small-body-text");
      var dateSpan = new Span(ds.connectedOn().toString());
      dateSpan.addClassNames("extra-small-body-text", "color-secondary");
      cell.add(userSpan, dateSpan);
      return cell;
    }).setHeader("Connected by").setFlexGrow(2);

    // Actions column
    grid.addComponentColumn(ds -> buildRowActions(ds, type)).setHeader("Actions").setFlexGrow(1);

    // populate
    refreshGrid(grid, type);

    return grid;
  }

  private Component buildRowActions(ConnectedDataset ds, String type) {
    var btnFactory = new ButtonFactory();

    var syncBtn = btnFactory.createIconButton(VaadinIcon.REFRESH.create());
    syncBtn.setTooltipText("Sync with " + ds.repository());
    syncBtn.addClickListener(e -> simulateSyncSingle(ds));

    var removeBtn = btnFactory.createIconButton(VaadinIcon.TRASH.create());
    removeBtn.setTooltipText("Remove connection");
    removeBtn.addClickListener(e -> openRemoveConfirmDialog(ds, type));

    var actions = new Div(syncBtn, removeBtn);
    actions.addClassNames("flex-horizontal", "gap-02", "no-flex-wrap");
    return actions;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  SEARCH & CONNECT DIALOG  (Stories 01 / 05)
  // ═══════════════════════════════════════════════════════════════════════════

  private void openSearchDialog(String type) {
    boolean isRestricted = "restricted".equals(type);

    // Story 05: warn if no token configured (simulated via a dismissible info box)
    if (isRestricted) {
      // In production this would check the user's token configuration.
      // For the prototype we show the warning but still allow proceeding.
    }

    AppDialog dialog = AppDialog.large();
    var titleText = isRestricted ? "Search Restricted Datasets" : "Search Public Datasets";
    DialogHeader.withIcon(dialog, titleText, VaadinIcon.SEARCH.create());

    var searchInput = new SearchDatasetInput(isRestricted);
    DialogBody.with(dialog, searchInput, searchInput);
    DialogFooter.with(dialog, "Cancel", "Connect");

    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      var selected = searchInput.getSelectedResult();
      if (selected == null) {
        showErrorNotification("Please select a dataset to connect.");
        return;
      }
      connectDataset(selected);
      dialog.close();
    });

    dialog.open();
  }

  /**
   * Connects a search result to the project (simulated in-memory).
   */
  private void connectDataset(SearchResult result) {
    // Prevent duplicates
    boolean alreadyConnected = connectedDatasets.stream()
        .anyMatch(ds -> ds.doi().equals(result.doi()));
    if (alreadyConnected) {
      showErrorNotification("\"" + result.title() + "\" is already connected to this project.");
      return;
    }
    connectedDatasets.add(new ConnectedDataset(
        result.doi(),
        result.title(),
        result.repository(),
        result.type(),
        "demo.user@uni-tuebingen.de",
        LocalDate.now(),
        result.version(),
        result.type().equals("restricted")
            ? "https://restricted-example.org/" + result.doi()
            : "https://zenodo.org/records/" + result.doi().replace("10.5281/zenodo.", "")
    ));
    refreshAllGrids();
    showSuccessNotification("\"" + result.title() + "\" successfully connected.");
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  REMOVE CONFIRMATION DIALOG  (Stories 03 / 07)
  // ═══════════════════════════════════════════════════════════════════════════

  private void openRemoveConfirmDialog(ConnectedDataset ds, String type) {
    AppDialog dialog = AppDialog.small();
    DialogHeader.withIcon(dialog, "Remove Dataset Connection", IconFactory.warningIcon());

    var bodyText = new Div(
        "Do you want to remove the connection to \"" + ds.title() + "\" ("
            + ds.doi() + ")? The dataset itself will not be deleted from "
            + ds.repository() + ".");
    bodyText.addClassName("normal-body-text");
    DialogBody.withoutUserInput(dialog, bodyText);
    DialogFooter.withDangerousConfirm(dialog, "Cancel", "Remove");

    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      boolean removed = connectedDatasets.removeIf(d -> d.doi().equals(ds.doi()));
      dialog.close();
      if (removed) {
        refreshAllGrids();
        showSuccessNotification("Connection to \"" + ds.title() + "\" removed.");
      } else {
        showErrorNotification("Could not remove the connection. Please try again.");
      }
    });

    dialog.open();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  SYNC SIMULATION  (Stories 04 / 08)
  // ═══════════════════════════════════════════════════════════════════════════

  private void simulateSyncSingle(ConnectedDataset ds) {
    // Simulate: dataset already on latest version → no update notification
    showSuccessNotification("\"" + ds.title() + "\" is already up to date (version " + ds.version() + ").");
  }

  private void simulateSyncAll(String type) {
    long count = connectedDatasets.stream()
        .filter(ds -> ds.type().equals(type))
        .count();
    if (count == 0) {
      showInfoNotification("No " + type + " datasets connected – nothing to synchronise.");
      return;
    }
    // Simulate one dataset getting a version bump for visual effect
    showSuccessNotification("Synchronisation complete. " + count + " dataset(s) checked, 0 updated.");
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  GRID REFRESH HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private void refreshAllGrids() {
    refreshGrid(publicGrid, "public");
    refreshGrid(restrictedGrid, "restricted");
  }

  private void refreshGrid(Grid<ConnectedDataset> grid, String type) {
    var items = connectedDatasets.stream()
        .filter(ds -> ds.type().equals(type))
        .collect(Collectors.toList());
    grid.setItems(items);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  NOTIFICATION HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private static void showSuccessNotification(String message) {
    var n = Notification.show(message, 4000, Position.BOTTOM_START);
    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
  }

  private static void showErrorNotification(String message) {
    var n = Notification.show(message, 5000, Position.BOTTOM_START);
    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
  }

  private static void showInfoNotification(String message) {
    Notification.show(message, 4000, Position.BOTTOM_START);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  INNER CLASS: Search Dataset User Input (dialog body)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * The content of the connect-dataset dialog. Provides:
   * <ul>
   *   <li>InvenioRDM instance selector (Zenodo / FDAT)</li>
   *   <li>Free-text search field</li>
   *   <li>Paginated result grid</li>
   *   <li>For restricted datasets: info box about required access token</li>
   * </ul>
   *
   * <p>Implements {@link UserInput} so the dialog can validate selection before confirming.</p>
   */
  static final class SearchDatasetInput extends Div implements UserInput {

    private SearchResult selectedResult = null;
    private final List<SearchResult> allResults;

    SearchDatasetInput(boolean restricted) {
      this.allResults = restricted
          ? new ArrayList<>(STUB_RESTRICTED_RESULTS)
          : new ArrayList<>(STUB_PUBLIC_RESULTS);

      addClassNames(FLEX_V, GAP_04);

      // ── Story 05: token warning for restricted datasets ──────────────────
      if (restricted) {
        var tokenWarning = new InfoBox()
            .setInfoText(
                "You need a valid InvenioRDM access token configured in your account "
                    + "settings to search restricted datasets. Navigate to Account → "
                    + "Personal Access Tokens to add one.")
            .setClosable(true);
        add(tokenWarning);
      }

      // ── Repository selector ──────────────────────────────────────────────
      var repoSection = DialogSection.with(
          "InvenioRDM Instance",
          "Select the repository you want to search in.");

      var repoSelect = new ComboBox<String>("Repository");
      repoSelect.setItems("All", "Zenodo", "FDAT");
      repoSelect.setValue("All");
      repoSelect.setWidth("16rem");
      repoSection.content(repoSelect);
      add(repoSection);

      // ── Search field ─────────────────────────────────────────────────────
      var searchSection = DialogSection.with(
          "Search",
          "Enter a search term to filter datasets, or leave empty to browse all.");

      var searchField = new TextField();
      searchField.setPlaceholder("e.g. proteomics, metadata, figures…");
      searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
      searchField.setClearButtonVisible(true);
      searchField.setValueChangeMode(ValueChangeMode.LAZY);
      searchField.setWidth("100%");
      searchSection.content(searchField);
      add(searchSection);

      // ── Results grid ─────────────────────────────────────────────────────
      var resultsSection = DialogSection.with(
          "Results",
          "Click a row to select a dataset, then confirm with Connect.");

      var grid = new Grid<SearchResult>();
      grid.setSelectionMode(Grid.SelectionMode.SINGLE);
      grid.addClassName("dataset-search-grid");

      grid.addColumn(SearchResult::title).setHeader("Title").setFlexGrow(3);
      grid.addColumn(SearchResult::doi).setHeader("DOI").setFlexGrow(2);
      grid.addColumn(SearchResult::creators).setHeader("Creators").setFlexGrow(2);
      grid.addComponentColumn(r -> {
        var tag = new Tag(r.repository());
        tag.setTagColor(tagColorForRepository(r.repository()));
        return tag;
      }).setHeader("Repository").setFlexGrow(1);
      grid.addColumn(SearchResult::version).setHeader("Version").setFlexGrow(1);
      grid.setAllRowsVisible(true);
      grid.setItems(allResults);

      grid.addSelectionListener(event ->
          selectedResult = event.getFirstSelectedItem().orElse(null));

      resultsSection.content(grid);
      add(resultsSection);

      // ── Live filtering by search term + repository ───────────────────────
      Runnable applyFilter = () -> {
        String term = searchField.getValue().trim().toLowerCase();
        String repo = repoSelect.getValue();
        var filtered = allResults.stream()
            .filter(r -> (repo == null || "All".equals(repo) || r.repository().equals(repo)))
            .filter(r -> term.isEmpty()
                || r.title().toLowerCase().contains(term)
                || r.doi().toLowerCase().contains(term)
                || r.creators().toLowerCase().contains(term))
            .collect(Collectors.toList());
        grid.setItems(filtered);
        selectedResult = null;
      };

      searchField.addValueChangeListener(e -> applyFilter.run());
      repoSelect.addValueChangeListener(e -> applyFilter.run());
    }

    /** Returns the dataset row selected by the user (may be null if nothing selected). */
    SearchResult getSelectedResult() {
      return selectedResult;
    }

    @Override
    @NonNull
    public InputValidation validate() {
      if (selectedResult == null) {
        return InputValidation.failed();
      }
      return InputValidation.passed();
    }

    @Override
    public boolean hasChanges() {
      return selectedResult != null;
    }
  }
}
