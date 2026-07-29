package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import org.springframework.context.annotation.Profile;

/**
 * <b>Associated Datasets Demo</b>
 *
 * <p>Pure UI prototype for the "Connect datasets with research projects" feature.
 * Demonstrates the interaction patterns for searching, connecting, viewing,
 * syncing, and removing associated datasets from InvenioRDM instances
 * (e.g. Zenodo, FDAT).</p>
 *
 * <p>Covers user stories 01–08 from the feature specification.</p>
 *
 * <p>This view is only available with the {@code development} profile.</p>
 *
 * @since 1.12.0
 */
@Profile("development")
@Route("test-view/associated-datasets")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
public class AssociatedDatasetsDemo extends Div {

  private static final List<String> INVENIO_INSTANCES = List.of(
      "Zenodo (zenodo.org)",
      "FDAT (fdat.uni-tuebingen.de)"
  );

  record SearchableDataset(
      String id, String title, String doi, String creators,
      String publicationDate, String repository, String accessRight,
      String description) {}

  record ConnectedDataset(
      String id, String title, String doi, String connectedBy,
      LocalDate connectedOn, String repository, String accessRight,
      String latestVersion, boolean updateAvailable) {}

  // ── Mock search result data ─────────────────────────────────────────────

  private static final List<SearchableDataset> MOCK_PUBLIC_DATASETS = List.of(
      new SearchableDataset("zen-001",
          "High-resolution cryo-EM structure of the human 26S proteasome",
          "10.5281/zenodo.1234567", "M. Bauer, S. Fernandez, L. Chen",
          "2024-11-15", "Zenodo", "Open",
          "Cryo-electron microscopy structure at 2.8 Å resolution revealing the complete architecture of the human 26S proteasome complex."),
      new SearchableDataset("zen-002",
          "Proteomic profiling of T-cell receptor signaling in Jurkat cells",
          "10.5281/zenodo.1234568", "A. Müller, K. Tanaka",
          "2024-10-03", "Zenodo", "Open",
          "Mass spectrometry datasets and analysis scripts for TCR signaling pathway characterization."),
      new SearchableDataset("zen-003",
          "Multi-omics integration pipeline benchmark dataset",
          "10.5281/zenodo.1234569", "QBiC Consortium",
          "2025-01-20", "Zenodo", "Open",
          "Reference dataset for benchmarking multi-omics integration methods across transcriptomics, proteomics, and metabolomics layers."),
      new SearchableDataset("zen-004",
          "Supplementary figures for 'Metabolic rewiring in tumor microenvironment'",
          "10.5281/zenodo.1234570", "J. Park, R. Schmidt, T. Nguyen",
          "2025-02-10", "Zenodo", "Open",
          "Additional figures, source data, and analysis notebooks accompanying the publication on metabolic rewiring."),
      new SearchableDataset("zen-005",
          "NGS raw reads: Arabidopsis thaliana drought stress response",
          "10.5281/zenodo.1234571", "P. Garcia, H. Weber",
          "2024-08-22", "Zenodo", "Open",
          "Illumina paired-end RNA-Seq reads from Arabidopsis thaliana under drought and control conditions."),
      new SearchableDataset("fdat-001",
          "Quantitative phosphoproteomics of DNA damage response",
          "10.5281/fdat.9876543", "C. Klein, D. Patel",
          "2025-03-05", "FDAT", "Open",
          "TMT-labeled phosphoproteomics data from HEK293 cells treated with genotoxic agents."),
      new SearchableDataset("fdat-002",
          "Spatial transcriptomics atlas of human kidney development",
          "10.5281/fdat.9876544", "S. Yamamoto, F. Rossi, QBiC",
          "2025-04-12", "FDAT", "Open",
          "10x Visium spatial transcriptomics data from human kidney sections at multiple developmental stages.")
  );

  private static final List<SearchableDataset> MOCK_RESTRICTED_DATASETS = List.of(
      new SearchableDataset("zen-r01",
          "Clinical metabolomics data — Cohort A (embargo until 2026-12)",
          "10.5281/zenodo.2345601", "R. Schmidt, M. Bauer",
          "2025-06-01", "Zenodo", "Restricted",
          "Clinical metabolomics profiles from Cohort A participants. Access restricted pending ethics review completion."),
      new SearchableDataset("zen-r02",
          "Pre-publication proteomics: Novel biomarker candidates",
          "10.5281/zenodo.2345602", "K. Tanaka, A. Petrova",
          "2025-05-20", "Zenodo", "Restricted",
          "Discovery-phase proteomics data for novel biomarker candidates. Under peer review."),
      new SearchableDataset("fdat-r01",
          "Oncology panel sequencing — Phase II trial (controlled access)",
          "10.5281/fdat.8765401", "QBiC Clinical Collaboration",
          "2025-04-01", "FDAT", "Restricted",
          "Targeted sequencing data from Phase II clinical trial. Access requires data use agreement.")
  );

  // ── Mutable state ─────────────────────────────────────────────────────

  private final List<ConnectedDataset> connectedPublicDatasets = new ArrayList<>();
  private final List<ConnectedDataset> connectedRestrictedDatasets = new ArrayList<>();

  private Grid<SearchableDataset> publicSearchResultsGrid;
  private Grid<SearchableDataset> restrictedSearchResultsGrid;
  private Grid<ConnectedDataset> connectedPublicGrid;
  private Grid<ConnectedDataset> connectedRestrictedGrid;

  private final TextField publicSearchField = new TextField();
  private final TextField restrictedSearchField = new TextField();
  private final ComboBox<String> publicInstanceSelector = new ComboBox<>();
  private final ComboBox<String> restrictedInstanceSelector = new ComboBox<>();

  // Buttons referenced from multiple places
  private Button connectPublicButton;
  private Button connectRestrictedButton;
  private Button syncAllPublicButton;
  private Button syncAllRestrictedButton;

  public AssociatedDatasetsDemo() {
    addClassNames("padding-horizontal-07", "padding-vertical-04");
    addClassName("flex-vertical");

    var title = new Div("Associated Datasets — UI Prototype");
    title.addClassName("heading-1");
    add(title);

    var subtitle = new Div(
        "Demonstrates Stories 01–08 from the \"Connect datasets with research projects\" feature. "
            + "All data is mocked — no real API calls are made.");
    subtitle.addClassName("normal-body-text");
    subtitle.addClassName("color-secondary");
    add(subtitle);
    add(new Div()); // spacer

    seedConnectedDatasets();

    var tabSheet = new TabSheet();
    tabSheet.addClassName("experimental-sheet");
    tabSheet.setWidthFull();
    tabSheet.add("Public Datasets", buildPublicDatasetsTab());
    tabSheet.add("Restricted Datasets", buildRestrictedDatasetsTab());
    add(tabSheet);
  }

  // ══════════════════════════════════════════════════════════════════════
  //  PUBLIC DATASETS TAB  (Stories 01 – 04)
  // ══════════════════════════════════════════════════════════════════════

  private Component buildPublicDatasetsTab() {
    var container = new Div();
    container.addClassNames("flex-vertical", "gap-04");
    container.add(searchAndConnectPublicSection());
    container.add(connectedPublicDatasetsSection());
    return container;
  }

  /**
   * Story 01 — Searching and connecting open, published datasets.
   */
  private Section searchAndConnectPublicSection() {
    var section = new Section.SectionBuilder().build();

    // ── Header: Connect button in ActionBar, selection count in button text ─
    connectPublicButton = new Button("Connect", VaadinIcon.PLUS_CIRCLE.create());
    connectPublicButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectPublicButton.addClickListener(e -> connectSelectedPublicDatasets());
    connectPublicButton.setEnabled(false);

    var connectActionBar = new ActionBar();
    connectActionBar.addButton(connectPublicButton);

    var header = new SectionHeader(
        new SectionTitle("Search and Connect"),
        connectActionBar,
        new SectionNote(
            "Search for published datasets on InvenioRDM repositories and connect them to this project.")
    );
    header.enableControls();
    section.setHeader(header);

    // ── Content ───────────────────────────────────────────────────────
    var content = new SectionContent();

    // Compact inline search bar: [Repository combo] [Search field] [Search button]
    var searchBar = new Div();
    searchBar.addClassNames("flex-horizontal", "gap-03", "width-full");

    publicInstanceSelector.setItems(INVENIO_INSTANCES);
    publicInstanceSelector.setPlaceholder("Select repository…");
    publicInstanceSelector.setValue(INVENIO_INSTANCES.get(0));
    publicInstanceSelector.setWidth("280px");
    publicInstanceSelector.setLabel("Repository");

    publicSearchField.setPlaceholder("Search by title, DOI, or author…");
    publicSearchField.setWidthFull();
    publicSearchField.setLabel("Search Term");
    publicSearchField.setClearButtonVisible(true);

    var searchButton = new Button("Search", VaadinIcon.SEARCH.create());
    searchButton.addClassName("margin-top-auto");
    searchButton.addClickListener(e -> performPublicSearch());

    searchBar.add(publicInstanceSelector, publicSearchField, searchButton);

    // Info note
    var infoNote = new InfoBox()
        .setInfoText("Public datasets do not require authentication. Anyone with write access to this project can connect datasets.");

    // Search results grid
    publicSearchResultsGrid = createSearchResultsGrid();
    populatePublicSearchResults("");

    publicSearchResultsGrid.addSelectionListener(event -> {
      int count = event.getAllSelectedItems().size();
      connectPublicButton.setEnabled(count > 0);
      connectPublicButton.setText(
          count == 0 ? "Connect"
          : count == 1 ? "Connect (1)"
          : "Connect (" + count + ")");
    });

    content.add(searchBar, infoNote, publicSearchResultsGrid);
    section.setContent(content);
    return section;
  }

  /**
   * Stories 02, 03, 04 — Viewing, removing, and syncing connected public datasets.
   */
  private Section connectedPublicDatasetsSection() {
    var section = new Section.SectionBuilder().build();

    syncAllPublicButton = new Button("Sync All", VaadinIcon.REFRESH.create());
    syncAllPublicButton.addClickListener(e -> syncAllPublic());

    var header = new SectionHeader(
        new SectionTitle("Connected Public Datasets"),
        new ActionBar(syncAllPublicButton),
        new SectionNote("Datasets connected from public InvenioRDM repositories.")
    );
    header.enableControls();
    section.setHeader(header);

    var content = new SectionContent();
    connectedPublicGrid = createConnectedDatasetsGrid();
    refreshConnectedPublicGrid();
    content.add(connectedPublicGrid);
    section.setContent(content);
    return section;
  }

  // ══════════════════════════════════════════════════════════════════════
  //  RESTRICTED DATASETS TAB  (Stories 05 – 08)
  // ══════════════════════════════════════════════════════════════════════

  private Component buildRestrictedDatasetsTab() {
    var container = new Div();
    container.addClassNames("flex-vertical", "gap-04");
    container.add(searchAndConnectRestrictedSection());
    container.add(connectedRestrictedDatasetsSection());
    return container;
  }

  /**
   * Story 05 — Searching and connecting restricted datasets.
   */
  private Section searchAndConnectRestrictedSection() {
    var section = new Section.SectionBuilder().build();

    connectRestrictedButton = new Button("Connect", VaadinIcon.PLUS_CIRCLE.create());
    connectRestrictedButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectRestrictedButton.addClickListener(e -> connectSelectedRestrictedDatasets());
    connectRestrictedButton.setEnabled(false);

    var connectActionBar = new ActionBar();
    connectActionBar.addButton(connectRestrictedButton);

    var header = new SectionHeader(
        new SectionTitle("Search and Connect"),
        connectActionBar,
        new SectionNote(
            "Search for access-restricted datasets on InvenioRDM repositories. Requires an authorization token configured in your account.")
    );
    header.enableControls();
    section.setHeader(header);

    // ── Content ───────────────────────────────────────────────────────
    var content = new SectionContent();

    // Token warning card (Story 05 — last acceptance criterion)
    var tokenWarning = new Div();
    tokenWarning.addClassNames("flex-horizontal", "gap-03", "border", "padding-03",
        "rounded-02");
    tokenWarning.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
    var warningIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
    warningIcon.addClassName("icon-color-warning");
    var warningText = new Span("No InvenioRDM authorization token configured. ");
    warningText.addClassName("normal-body-text");
    var configureLink = new Anchor("#/account/tokens", "Configure your token");
    configureLink.addClassName("normal-body-text");
    tokenWarning.add(warningIcon, warningText, configureLink);
    for (Component child : tokenWarning.getChildren().toList()) {
      child.addClassName("margin-top-auto");
      child.addClassName("margin-bottom-auto");
    }
    content.add(tokenWarning);

    // Compact inline search bar
    var searchBar = new Div();
    searchBar.addClassNames("flex-horizontal", "gap-03", "width-full");

    restrictedInstanceSelector.setItems(INVENIO_INSTANCES);
    restrictedInstanceSelector.setPlaceholder("Select repository…");
    restrictedInstanceSelector.setValue(INVENIO_INSTANCES.get(0));
    restrictedInstanceSelector.setWidth("280px");
    restrictedInstanceSelector.setLabel("Repository");

    restrictedSearchField.setPlaceholder("Search by title, DOI, or author…");
    restrictedSearchField.setWidthFull();
    restrictedSearchField.setLabel("Search Term");
    restrictedSearchField.setClearButtonVisible(true);

    var searchButton = new Button("Search", VaadinIcon.SEARCH.create());
    searchButton.addClassName("margin-top-auto");
    searchButton.addClickListener(e -> performRestrictedSearch());

    searchBar.add(restrictedInstanceSelector, restrictedSearchField, searchButton);

    // Search results grid
    restrictedSearchResultsGrid = createSearchResultsGrid();
    populateRestrictedSearchResults("");

    restrictedSearchResultsGrid.addSelectionListener(event -> {
      int count = event.getAllSelectedItems().size();
      connectRestrictedButton.setEnabled(count > 0);
      connectRestrictedButton.setText(
          count == 0 ? "Connect"
          : count == 1 ? "Connect (1)"
          : "Connect (" + count + ")");
    });

    // Info note
    var infoNote = new InfoBox()
        .setInfoText("Restricted datasets require a valid authorization token. Connected project members can access them via the provided access link.");

    content.add(searchBar, restrictedSearchResultsGrid, infoNote);
    section.setContent(content);
    return section;
  }

  /**
   * Stories 06, 07, 08 — Viewing, removing, and syncing connected restricted datasets.
   */
  private Section connectedRestrictedDatasetsSection() {
    var section = new Section.SectionBuilder().build();

    syncAllRestrictedButton = new Button("Sync All", VaadinIcon.REFRESH.create());
    syncAllRestrictedButton.addClickListener(e -> syncAllRestricted());

    var header = new SectionHeader(
        new SectionTitle("Connected Restricted Datasets"),
        new ActionBar(syncAllRestrictedButton),
        new SectionNote("Datasets connected from InvenioRDM repositories with restricted access.")
    );
    header.enableControls();
    section.setHeader(header);

    var content = new SectionContent();
    connectedRestrictedGrid = createConnectedDatasetsGrid();
    refreshConnectedRestrictedGrid();
    content.add(connectedRestrictedGrid);
    section.setContent(content);
    return section;
  }

  // ══════════════════════════════════════════════════════════════════════
  //  GRID BUILDERS
  // ═════════════════════════════════════════════════════════════════════

  private Grid<SearchableDataset> createSearchResultsGrid() {
    var grid = new Grid<SearchableDataset>();
    grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
    grid.setSelectionMode(SelectionMode.MULTI);
    grid.setWidthFull();
    grid.setAllRowsVisible(true);

    // Title + creators column
    grid.addComponentColumn(dataset -> {
      var wrapper = new Div();
      wrapper.addClassNames("flex-vertical", "gap-01");
      var titleSpan = new Span(dataset.title());
      titleSpan.addClassName("normal-body-text");
      titleSpan.getStyle().set("font-weight", "500");
      var creatorsSpan = new Span(dataset.creators());
      creatorsSpan.addClassName("extra-small-body-text");
      creatorsSpan.addClassName("color-secondary");
      wrapper.add(titleSpan, creatorsSpan);
      return wrapper;
    }).setHeader("Title / Creators").setAutoWidth(true).setFlexGrow(2).setKey("dataset");

    // DOI column
    grid.addComponentColumn(dataset -> {
      var anchor = new Anchor("https://doi.org/" + dataset.doi(), dataset.doi());
      anchor.setTarget(AnchorTarget.BLANK);
      anchor.addClassName("extra-small-body-text");
      return anchor;
    }).setHeader("DOI").setAutoWidth(true).setFlexGrow(1).setKey("doi");

    // Date column
    grid.addColumn(ds -> ds.publicationDate())
        .setHeader("Published").setAutoWidth(true).setKey("date");

    // Repository column
    grid.addComponentColumn(dataset -> {
      var tag = new Tag(dataset.repository());
      if ("Zenodo".equals(dataset.repository())) {
        tag.setTagColor(TagColor.PRIMARY);
      } else {
        tag.setTagColor(TagColor.TEAL);
      }
      return tag;
    }).setHeader("Repository").setAutoWidth(true).setKey("repository");

    // Access type column
    grid.addComponentColumn(dataset -> {
      var tag = new Tag(dataset.accessRight());
      if ("Open".equals(dataset.accessRight())) {
        tag.setTagColor(TagColor.SUCCESS);
      } else {
        tag.setTagColor(TagColor.WARNING);
      }
      return tag;
    }).setHeader("Access").setAutoWidth(true).setKey("access");

    return grid;
  }

  private Grid<ConnectedDataset> createConnectedDatasetsGrid() {
    var grid = new Grid<ConnectedDataset>();
    grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
    grid.setSelectionMode(SelectionMode.NONE);
    grid.setWidthFull();

    // Dataset column: title + DOI + update banner
    grid.addComponentColumn(dataset -> {
      var wrapper = new Div();
      wrapper.addClassNames("flex-vertical", "gap-01");
      var titleSpan = new Span(dataset.title());
      titleSpan.addClassName("normal-body-text");
      titleSpan.getStyle().set("font-weight", "500");
      var doiSpan = new Span(dataset.doi());
      doiSpan.addClassName("extra-small-body-text");
      doiSpan.addClassName("color-secondary");
      wrapper.add(titleSpan, doiSpan);
      if (dataset.updateAvailable()) {
        var updateTag = new Tag("Update available → v" + dataset.latestVersion());
        updateTag.setTagColor(TagColor.WARNING);
        wrapper.add(updateTag);
      }
      return wrapper;
    }).setHeader("Dataset").setFlexGrow(2).setKey("dataset");

    // Connected By: user + date stacked
    grid.addComponentColumn(dataset -> {
      var wrapper = new Div();
      wrapper.addClassNames("flex-vertical", "gap-01");
      var userSpan = new Span(dataset.connectedBy());
      userSpan.addClassName("normal-body-text");
      var dateSpan = new Span(
          dataset.connectedOn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      dateSpan.addClassName("extra-small-body-text");
      dateSpan.addClassName("color-secondary");
      wrapper.add(userSpan, dateSpan);
      return wrapper;
    }).setHeader("Connected By").setAutoWidth(true).setKey("connectedBy");

    // Repository column
    grid.addComponentColumn(dataset -> {
      var tag = new Tag(dataset.repository());
      if ("Zenodo".equals(dataset.repository())) {
        tag.setTagColor(TagColor.PRIMARY);
      } else {
        tag.setTagColor(TagColor.TEAL);
      }
      return tag;
    }).setHeader("Repository").setAutoWidth(true).setKey("repository");

    // Access Link column
    grid.addComponentColumn(dataset -> {
      var link = new Anchor(
          "https://doi.org/" + dataset.doi() + "?access=project-token",
          "Open");
      link.setTarget(AnchorTarget.BLANK);
      link.addClassName("extra-small-body-text");
      var wrapper = new Div();
      wrapper.addClassNames("flex-vertical");
      wrapper.add(link);
      var note = new Span("No account required");
      note.addClassName("extra-small-body-text");
      note.addClassName("color-secondary");
      wrapper.add(note);
      return wrapper;
    }).setHeader("Access Link").setAutoWidth(true).setKey("accessLink");

    // Actions column: Sync + Remove
    grid.addComponentColumn(dataset -> {
      var syncButton = new Button(VaadinIcon.REFRESH.create());
      syncButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      syncButton.setTooltipText("Sync with " + dataset.repository());
      syncButton.getStyle().set("padding", "var(--lumo-space-s)");
      syncButton.addClickListener(e -> syncSingleDataset(dataset));

      var removeButton = new Button(VaadinIcon.TRASH.create());
      removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
      removeButton.setTooltipText("Remove connection");
      removeButton.getStyle().set("padding", "var(--lumo-space-s)");
      removeButton.addClickListener(e -> confirmRemoveDataset(dataset));

      var wrapper = new Div();
      wrapper.addClassNames("flex-horizontal", "gap-01");
      wrapper.add(syncButton, removeButton);
      return wrapper;
    }).setHeader("Actions").setAutoWidth(true).setKey("actions");

    return grid;
  }

  // ══════════════════════════════════════════════════════════════════════
  //  SEARCH LOGIC (mock)
  // ══════════════════════════════════════════════════════════════════════

  private void performPublicSearch() {
    populatePublicSearchResults(publicSearchField.getValue());
  }

  private void populatePublicSearchResults(String term) {
    publicSearchResultsGrid.setItems(filterDatasets(MOCK_PUBLIC_DATASETS, term));
  }

  private void performRestrictedSearch() {
    populateRestrictedSearchResults(restrictedSearchField.getValue());
  }

  private void populateRestrictedSearchResults(String term) {
    restrictedSearchResultsGrid.setItems(filterDatasets(MOCK_RESTRICTED_DATASETS, term));
  }

  private List<SearchableDataset> filterDatasets(List<SearchableDataset> datasets, String term) {
    if (term == null || term.isBlank()) {
      return new ArrayList<>(datasets);
    }
    String lower = term.toLowerCase();
    return datasets.stream()
        .filter(d -> d.title().toLowerCase().contains(lower)
            || d.doi().toLowerCase().contains(lower)
            || d.creators().toLowerCase().contains(lower)
            || d.description().toLowerCase().contains(lower))
        .toList();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CONNECT / REMOVE / SYNC (mock)
  // ══════════════════════════════════════════════════════════════════════

  private void connectSelectedPublicDatasets() {
    var selected = publicSearchResultsGrid.getSelectedItems();
    if (selected.isEmpty()) {
      return;
    }
    for (SearchableDataset ds : selected) {
      connectedPublicDatasets.add(
          new ConnectedDataset(ds.id(), ds.title(), ds.doi(),
              "Current User (demo)", LocalDate.now(), ds.repository(),
              ds.accessRight(), "1.0", false));
    }
    refreshConnectedPublicGrid();
    publicSearchResultsGrid.deselectAll();
    connectPublicButton.setEnabled(false);
    connectPublicButton.setText("Connect");
    showSuccessNotification(selected.size()
        + " public dataset(s) connected to this project.");
  }

  private void connectSelectedRestrictedDatasets() {
    var selected = restrictedSearchResultsGrid.getSelectedItems();
    if (selected.isEmpty()) {
      return;
    }
    for (SearchableDataset ds : selected) {
      connectedRestrictedDatasets.add(
          new ConnectedDataset(ds.id(), ds.title(), ds.doi(),
              "Current User (demo)", LocalDate.now(), ds.repository(),
              ds.accessRight(), "1.0", false));
    }
    refreshConnectedRestrictedGrid();
    restrictedSearchResultsGrid.deselectAll();
    connectRestrictedButton.setEnabled(false);
    connectRestrictedButton.setText("Connect");
    showSuccessNotification(selected.size()
        + " restricted dataset(s) connected to this project.");
  }

  private void confirmRemoveDataset(ConnectedDataset dataset) {
    var dialog = new Dialog();
    dialog.setCloseOnOutsideClick(false);
    dialog.setCloseOnEsc(false);

    var headerDiv = new Div();
    headerDiv.addClassNames("flex-horizontal", "gap-03", "padding-horizontal-07",
        "padding-vertical-04");
    var icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
    icon.addClassName("icon-color-warning");
    var title = new Span("Remove Dataset Connection");
    title.addClassName("heading-3");
    headerDiv.add(icon, title);

    var body = new Div();
    body.addClassNames("flex-vertical", "gap-03", "padding-horizontal-07", "padding-vertical-04");
    body.add(new Span("Are you sure you want to remove the connection to:"));
    var dsLabel = new Span(dataset.title());
    dsLabel.getStyle().set("font-weight", "600");
    body.add(dsLabel);
    body.add(new Span("This will not delete the dataset on " + dataset.repository()
        + " — it only removes the link from this project."));

    var footer = new Div();
    footer.addClassNames("flex-horizontal", "gap-03", "padding-horizontal-07",
        "padding-vertical-04");
    footer.getStyle().set("justify-content", "flex-end");
    var cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    var removeBtn = new Button("Remove", VaadinIcon.TRASH.create(), e -> {
      removeDataset(dataset);
      dialog.close();
    });
    removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
    footer.add(cancelBtn, removeBtn);

    dialog.add(headerDiv, body);
    dialog.getFooter().add(footer);
    dialog.open();
  }

  private void removeDataset(ConnectedDataset dataset) {
    connectedPublicDatasets.removeIf(d -> d.id().equals(dataset.id()));
    connectedRestrictedDatasets.removeIf(d -> d.id().equals(dataset.id()));
    refreshConnectedPublicGrid();
    refreshConnectedRestrictedGrid();
    showSuccessNotification("Dataset connection removed: " + dataset.title());
  }

  private void syncSingleDataset(ConnectedDataset dataset) {
    showInfoNotification("Syncing " + dataset.title() + " with " + dataset.repository() + "…");
    var ui = UI.getCurrent();
    new Thread(() -> {
      try {
        Thread.sleep(1500);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      ui.access(() -> {
        simulateUpdateAvailable(dataset);
        showInfoNotification("Sync complete for '" + dataset.title() + "'. Version is up to date.");
      });
    }).start();
  }

  private void simulateUpdateAvailable(ConnectedDataset dataset) {
    for (int i = 0; i < connectedPublicDatasets.size(); i++) {
      if (connectedPublicDatasets.get(i).id().equals(dataset.id())) {
        var old = connectedPublicDatasets.get(i);
        connectedPublicDatasets.set(i,
            new ConnectedDataset(old.id(), old.title(), old.doi(), old.connectedBy(),
                old.connectedOn(), old.repository(), old.accessRight(), "1.1", true));
        break;
      }
    }
    for (int i = 0; i < connectedRestrictedDatasets.size(); i++) {
      if (connectedRestrictedDatasets.get(i).id().equals(dataset.id())) {
        var old = connectedRestrictedDatasets.get(i);
        connectedRestrictedDatasets.set(i,
            new ConnectedDataset(old.id(), old.title(), old.doi(), old.connectedBy(),
                old.connectedOn(), old.repository(), old.accessRight(), "1.1", true));
        break;
      }
    }
    refreshConnectedPublicGrid();
    refreshConnectedRestrictedGrid();
  }

  private void syncAllPublic() {
    if (connectedPublicDatasets.isEmpty()) {
      showInfoNotification("No public datasets connected to sync.");
      return;
    }
    showInfoNotification("Syncing all " + connectedPublicDatasets.size()
        + " public datasets…");
    showSuccessNotification("All " + connectedPublicDatasets.size()
        + " public datasets are up to date.");
  }

  private void syncAllRestricted() {
    if (connectedRestrictedDatasets.isEmpty()) {
      showInfoNotification("No restricted datasets connected to sync.");
      return;
    }
    showInfoNotification("Syncing all " + connectedRestrictedDatasets.size()
        + " restricted datasets…");
    showSuccessNotification("All " + connectedRestrictedDatasets.size()
        + " restricted datasets are up to date.");
  }

  private void refreshConnectedPublicGrid() {
    connectedPublicGrid.setItems(new ArrayList<>(connectedPublicDatasets));
  }

  private void refreshConnectedRestrictedGrid() {
    connectedRestrictedGrid.setItems(new ArrayList<>(connectedRestrictedDatasets));
  }

  // ══════════════════════════════════════════════════════════════════════
  //  NOTIFICATION HELPERS
  // ══════════════════════════════════════════════════════════════════════

  private void showSuccessNotification(String message) {
    var notification = new Notification(message, 3000);
    notification.addClassName("success-toast");
    notification.setPosition(Notification.Position.BOTTOM_END);
    notification.open();
  }

  private void showInfoNotification(String message) {
    var notification = new Notification(message, 3000);
    notification.addClassName("info-toast");
    notification.setPosition(Notification.Position.BOTTOM_END);
    notification.open();
  }

  private void showErrorNotification(String message) {
    var notification = new Notification(message, 5000);
    notification.addClassName("error-toast");
    notification.setPosition(Notification.Position.BOTTOM_END);
    notification.open();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  SEED DATA
  // ══════════════════════════════════════════════════════════════════════

  private void seedConnectedDatasets() {
    connectedPublicDatasets.add(
        new ConnectedDataset("zen-seed-01",
            "Benchmark dataset for reproducibility assessment",
            "10.5281/zenodo.9999001", "Alice Schmidt",
            LocalDate.of(2025, 1, 15), "Zenodo", "Open", "2.0", false));
    connectedPublicDatasets.add(
        new ConnectedDataset("zen-seed-02",
            "Metabolomics reference spectra library",
            "10.5281/zenodo.9999002", "Bob Fernandez",
            LocalDate.of(2025, 3, 22), "Zenodo", "Open", "1.2", true));
    connectedRestrictedDatasets.add(
        new ConnectedDataset("fdat-seed-01",
            "Multi-center clinical proteomics study (Controlled)",
            "10.5281/fdat.7777001", "Carol Yamamoto",
            LocalDate.of(2025, 5, 8), "FDAT", "Restricted", "1.0", false));
  }
}
