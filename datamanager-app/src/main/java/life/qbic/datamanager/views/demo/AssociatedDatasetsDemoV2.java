package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
 * <b>Associated Datasets Demo V2 — Sidebar Prototype</b>
 *
 * <p>Pure UI prototype for the "Connect datasets with research projects" feature.
 * This second iteration addresses feedback from the first prototype:</p>
 *
 * <ul>
 *   <li><b>No modal for connecting datasets</b> — uses a right-side sliding sidebar panel
 *       that works within the existing AppLayout drawer/content structure.</li>
 *   <li><b>Unified resource view</b> — one grid shows all connected resources regardless
 *       of access level. A segmented filter lets the user show All / Public / Restricted.</li>
 *   <li><b>Access level inline</b> — each resource row displays its access status
 *       (public vs. restricted) as an inline badge, eliminating duplicate sections.</li>
 *   <li><b>Stakeholder-aligned properties</b> — high-prio properties (Title, PID, Access
 *       Status, Version, Access Link, Publication Date) are shown in the main grid;
 *       medium-prio properties (Connected By, Resource Provider, Creator, Resource Type,
 *       Community, Linked Experiment) are shown in an expandable detail row.</li>
 * </ul>
 *
 * <p>Covers user stories 01–08 from the feature specification.</p>
 *
 * <p>This view is only available with the {@code development} profile.</p>
 *
 * @since 1.12.0
 */
@Profile("development")
@Route("test-view/associated-datasets-v2")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
public class AssociatedDatasetsDemoV2 extends Div {

  private static final List<String> INVENIO_INSTANCES = List.of(
      "Zenodo (zenodo.org)",
      "FDAT (fdat.uni-tuebingen.de)"
  );

  // ── Domain records ────────────────────────────────────────────────────

  enum AccessLevel { PUBLIC, RESTRICTED }

  /** A connected dataset resource displayed in the main grid. */
  record ConnectedResource(
      // High-prio (from stakeholder doc)
      String title,
      String pid,
      AccessLevel accessLevel,       // Covers "Record: public/restricted" + "Files: restricted"
      String version,
      String accessLink,
      LocalDate publicationDate,
      // Medium-prio (from stakeholder doc)
      String connectedBy,
      String resourceProvider,
      String creator,
      String resourceType,
      String community,
      String linkedExperiment,
      // Internal
      String id,
      LocalDate connectedOn,
      boolean updateAvailable
  ) {}

  /** A dataset found via search, available to be connected. */
  record SearchableResource(
      String id,
      String title,
      String pid,
      AccessLevel accessLevel,
      String version,
      String accessLink,
      LocalDate publicationDate,
      String resourceProvider,
      String creator,
      String resourceType,
      String community,
      String description
  ) {}

  // ── Mock data ─────────────────────────────────────────────────────────

  private static final List<SearchableResource> MOCK_SEARCH_RESULTS = List.of(
      new SearchableResource("zen-001",
          "High-resolution cryo-EM structure of the human 26S proteasome",
          "10.5281/zenodo.1234567", AccessLevel.PUBLIC,
          "v1", "https://zenodo.org/records/1234567",
          LocalDate.of(2024, 11, 15), "Zenodo", "M. Bauer, S. Fernandez",
          "Dataset", null,
          "Cryo-electron microscopy structure at 2.8 Å resolution."),
      new SearchableResource("zen-002",
          "Proteomic profiling of T-cell receptor signaling in Jurkat cells",
          "10.5281/zenodo.1234568", AccessLevel.PUBLIC,
          "v1", "https://zenodo.org/records/1234568",
          LocalDate.of(2024, 10, 3), "Zenodo", "A. Müller, K. Tanaka",
          "Dataset", null,
          "Mass spectrometry datasets for TCR signaling pathway characterization."),
      new SearchableResource("zen-003",
          "Multi-omics integration pipeline benchmark dataset",
          "10.5281/zenodo.1234569", AccessLevel.PUBLIC,
          "v1", "https://zenodo.org/records/1234569",
          LocalDate.of(2025, 1, 20), "Zenodo", "QBiC Consortium",
          "Dataset", "QBiC",
          "Reference dataset for benchmarking multi-omics integration methods."),
      new SearchableResource("fdat-001",
          "Quantitative phosphoproteomics of DNA damage response",
          "10.5281/fdat.9876543", AccessLevel.PUBLIC,
          "v1", "https://fdat.uni-tuebingen.de/records/9876543",
          LocalDate.of(2025, 3, 5), "FDAT", "C. Klein, D. Patel",
          "Dataset", "CMFI",
          "TMT-labeled phosphoproteomics data from HEK293 cells."),
      new SearchableResource("zen-r01",
          "Clinical metabolomics data — Cohort A (embargo until 2026-12)",
          "10.5281/zenodo.2345601", AccessLevel.RESTRICTED,
          "v1", "https://zenodo.org/records/2345601",
          LocalDate.of(2025, 6, 1), "Zenodo", "R. Schmidt, M. Bauer",
          "Dataset", null,
          "Clinical metabolomics profiles. Access restricted pending ethics review."),
      new SearchableResource("fdat-r01",
          "Oncology panel sequencing — Phase II trial (controlled access)",
          "10.5281/fdat.8765401", AccessLevel.RESTRICTED,
          "v1", "https://fdat.uni-tuebingen.de/records/8765401",
          LocalDate.of(2025, 4, 1), "FDAT", "QBiC Clinical Collaboration",
          "Dataset", "SFB209",
          "Targeted sequencing data from Phase II clinical trial."),
      new SearchableResource("zen-004",
          "Supplementary figures for 'Metabolic rewiring in tumor microenvironment'",
          "10.5281/zenodo.1234570", AccessLevel.PUBLIC,
          "v1", "https://zenodo.org/records/1234570",
          LocalDate.of(2025, 2, 10), "Zenodo", "J. Park, R. Schmidt",
          "Publication", null,
          "Additional figures and source data accompanying the publication."),
      new SearchableResource("zen-r02",
          "Pre-publication proteomics: Novel biomarker candidates",
          "10.5281/zenodo.2345602", AccessLevel.RESTRICTED,
          "v1", "https://zenodo.org/records/2345602",
          LocalDate.of(2025, 5, 20), "Zenodo", "K. Tanaka, A. Petrova",
          "Dataset", null,
          "Discovery-phase proteomics data. Under peer review.")
  );

  // ── Mutable state ─────────────────────────────────────────────────────

  private final List<ConnectedResource> connectedResources = new ArrayList<>();
  private Grid<ConnectedResource> resourcesGrid;
  private final TextField searchField = new TextField();
  private final ComboBox<String> instanceSelector = new ComboBox<>();
  private RadioButtonGroup<String> accessFilter;
  private Button connectButton;
  private Grid<SearchableResource> searchResultsGrid;

  // Sidebar panel elements
  private Div sidebarOverlay;
  private Div sidebarPanel;
  private boolean sidebarOpen = false;
  private Span selectionCountLabel;
  private Button sidebarConnectBtn;

  public AssociatedDatasetsDemoV2() {
    addClassNames("padding-horizontal-07", "padding-vertical-04");
    addClassName("flex-vertical");

    seedConnectedResources();

    // ── Page heading ────────────────────────────────────────────────
    var title = new Div("Connected Resources — UI Prototype V2");
    title.addClassName("heading-1");
    add(title);

    var subtitle = new Div(
        "Sidebar approach: connecting datasets uses a sliding panel instead of a modal. "
            + "All resources are shown in a unified view with access level as an inline indicator.");
    subtitle.addClassName("normal-body-text");
    subtitle.addClassName("color-secondary");
    add(subtitle);
    add(new Div()); // spacer

    // ── Main content: Connected Resources section ───────────────────
    add(buildConnectedResourcesSection());

    // ── Sidebar overlay + panel (initially hidden) ──────────────────
    buildConnectSidebar();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CONNECTED RESOURCES — Main Section  (High-Prio properties)
  // ══════════════════════════════════════════════════════════════════════

  private Section buildConnectedResourcesSection() {
    var section = new Section.SectionBuilder().build();

    // ── ActionBar: Connect button + Sync All ────────────────────────
    connectButton = new Button("Connect Datasets", VaadinIcon.PLUS_CIRCLE.create());
    connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectButton.addClickListener(e -> openConnectSidebar());

    var syncAllButton = new Button("Sync All", VaadinIcon.REFRESH.create());
    syncAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    syncAllButton.addClickListener(e -> syncAllResources());

    var actionBar = new ActionBar();
    actionBar.addButton(connectButton);
    actionBar.addButton(syncAllButton);

    var header = new SectionHeader(
        new SectionTitle("Connected Resources"),
        actionBar,
        new SectionNote("Datasets connected from InvenioRDM repositories. "
            + "High-priority properties are shown below. Expand a row for details.")
    );
    header.enableControls();
    section.setHeader(header);

    // ── Filter bar: Access Level toggle ─────────────────────────────
    var content = new SectionContent();

    var filterBar = new Div();
    filterBar.addClassNames("flex-horizontal", "gap-03", "items-center");
    filterBar.getStyle().set("margin-bottom", "var(--lumo-space-m)");

    var filterLabel = new Span("Show:");
    filterLabel.addClassName("normal-body-text");
    filterLabel.getStyle().set("font-weight", "500");

    accessFilter = new RadioButtonGroup<>();
    accessFilter.setItems("All", "Public", "Restricted");
    accessFilter.setValue("All");
    accessFilter.addThemeVariants();
    accessFilter.addValueChangeListener(e -> refreshResourcesGrid());

    // Resource count badge
    var countBadge = new Span();
    countBadge.addClassName("extra-small-body-text");
    countBadge.addClassName("color-secondary");
    countBadge.getStyle().set("margin-left", "auto");
    updateResourceCount(countBadge);

    filterBar.add(filterLabel, accessFilter, countBadge);
    content.add(filterBar);

    // ── Unified resources grid ──────────────────────────────────────
    resourcesGrid = new Grid<>();
    resourcesGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
    resourcesGrid.setSelectionMode(SelectionMode.NONE);
    resourcesGrid.setWidthFull();

    // Expandable detail row for medium-prio properties
    resourcesGrid.setItemDetailsRenderer(
        new ComponentRenderer<>(resource -> {
          var detailPanel = new Div();
          detailPanel.addClassNames("flex-vertical", "gap-03");
          detailPanel.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
          detailPanel.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
          detailPanel.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

          var detailTitle = new Span("Additional Information");
          detailTitle.getStyle().set("font-weight", "600");
          detailTitle.addClassName("normal-body-text");
          detailPanel.add(detailTitle);

          var detailsGrid = new Div();
          detailsGrid.addClassNames("flex-vertical", "gap-02");

          addDetailRow(detailsGrid, "Connected By", resource.connectedBy());
          addDetailRow(detailsGrid, "Resource Provider", resource.resourceProvider());
          addDetailRow(detailsGrid, "Creator", resource.creator());
          addDetailRow(detailsGrid, "Resource Type", resource.resourceType());
          addDetailRow(detailsGrid, "Community",
              resource.community() != null ? resource.community() : "—");
          addDetailRow(detailsGrid, "Linked Experiment",
              resource.linkedExperiment() != null ? resource.linkedExperiment() : "—");

          detailPanel.add(detailsGrid);
          return detailPanel;
        }));

    // ── Columns (High-Prio from stakeholder doc) ────────────────────

    // Title column (expandable for details)
    resourcesGrid.addComponentColumn(resource -> {
      var wrapper = new Div();
      wrapper.addClassNames("flex-vertical", "gap-01");
      var titleSpan = new Span(resource.title());
      titleSpan.addClassName("normal-body-text");
      titleSpan.getStyle().set("font-weight", "500");
      wrapper.add(titleSpan);
      // Update hint inline: a subtle clickable banner that explains the situation
      // and directs the user to the Sync button.
      if (resource.updateAvailable()) {
        var updateHint = new Div();
        updateHint.getStyle().set("display", "inline-flex");
        updateHint.getStyle().set("align-items", "center");
        updateHint.getStyle().set("gap", "var(--lumo-space-xs)");
        updateHint.getStyle().set("padding",
            "var(--lumo-space-xxs) var(--lumo-space-s)");
        updateHint.getStyle().set("border-radius",
            "var(--lumo-border-radius-m)");
        updateHint.getStyle().set("background-color",
            "var(--lumo-warning-color-10pct)");
        updateHint.getStyle().set("border",
            "1px solid var(--lumo-warning-color)");
        updateHint.getStyle().set("cursor", "pointer");

        Icon exclamationIcon = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
        exclamationIcon.getStyle().set("color", "var(--lumo-warning-color)");
        exclamationIcon.getStyle().set("font-size", "var(--lumo-icon-size-s)");

        var hintText = new Span("New version available — sync to update");
        hintText.addClassName("extra-small-body-text");
        hintText.getStyle().set("color", "var(--lumo-warning-color)");
        hintText.getStyle().set("font-weight", "500");

        updateHint.add(exclamationIcon, hintText);
        updateHint.addClickListener(e -> syncSingleResource(resource));
        wrapper.add(updateHint);
      }
      return wrapper;
    }).setHeader("Title").setFlexGrow(3).setKey("title");

    // PID column (DOI)
    resourcesGrid.addComponentColumn(resource -> {
      var anchor = new Anchor(
          resource.pid().startsWith("http") ? resource.pid()
              : "https://doi.org/" + resource.pid(),
          resource.pid());
      anchor.setTarget(AnchorTarget.BLANK);
      anchor.addClassName("extra-small-body-text");
      return anchor;
    }).setHeader("PID / DOI").setAutoWidth(true).setFlexGrow(1).setKey("pid");

    // Access Status column — inline badge showing record + file access
    resourcesGrid.addComponentColumn(resource -> {
      return buildAccessStatusBadge(resource);
    }).setHeader("Access Status").setAutoWidth(true).setFlexGrow(0).setKey("access");

    // Version column
    resourcesGrid.addColumn(ConnectedResource::version)
        .setHeader("Version").setAutoWidth(true).setKey("version");

    // Access Link column
    resourcesGrid.addComponentColumn(resource -> {
      if (resource.accessLink() != null && !resource.accessLink().isBlank()) {
        var link = new Anchor(resource.accessLink(), "Open ↗");
        link.setTarget(AnchorTarget.BLANK);
        link.addClassName("extra-small-body-text");
        return link;
      }
      return new Span("—");
    }).setHeader("Access Link").setAutoWidth(true).setKey("accessLink");

    // Publication Date column
    resourcesGrid.addComponentColumn(resource -> {
      return new Span(resource.publicationDate().format(
          DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
    }).setHeader("Published").setAutoWidth(true).setKey("publicationDate");

    // Actions column: Sync + Remove
    resourcesGrid.addComponentColumn(resource -> {
      var syncBtn = new Button(VaadinIcon.REFRESH.create());
      if (resource.updateAvailable()) {
        // Visually indicate that a newer version is available:
        // icon gets warning color and tooltip explains what to do.
        syncBtn.getStyle().set("color", "var(--lumo-warning-color)");
        syncBtn.setTooltipText(
            "New version available on " + resource.resourceProvider()
                + ". Click to update this connection.");
      } else {
        syncBtn.setTooltipText("Check " + resource.resourceProvider()
            + " for updates.");
      }
      syncBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      syncBtn.getStyle().set("padding", "var(--lumo-space-s)");
      syncBtn.addClickListener(e -> syncSingleResource(resource));

      var removeBtn = new Button(VaadinIcon.TRASH.create());
      removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
      removeBtn.setTooltipText("Remove connection");
      removeBtn.getStyle().set("padding", "var(--lumo-space-s)");
      removeBtn.addClickListener(e -> confirmRemoveResource(resource));

      var wrapper = new Div();
      wrapper.addClassNames("flex-horizontal", "gap-01");
      wrapper.add(syncBtn, removeBtn);
      return wrapper;
    }).setHeader("Actions").setAutoWidth(true).setKey("actions");

    refreshResourcesGrid();
    content.add(resourcesGrid);
    section.setContent(content);
    return section;
  }

  /**
   * Builds a compact access status badge. Uses the stakeholder's distinction:
   * a record can have public metadata while the files themselves are restricted.
   * Format: "Record: public | Files: restricted"
   */
  private Component buildAccessStatusBadge(ConnectedResource resource) {
    var wrapper = new Div();
    wrapper.addClassNames("flex-vertical", "gap-01");

    if (resource.accessLevel() == AccessLevel.PUBLIC) {
      var badge = new Tag("Public");
      badge.setTagColor(TagColor.SUCCESS);
      wrapper.add(badge);
    } else {
      var badge = new Tag("Restricted");
      badge.setTagColor(TagColor.WARNING);
      wrapper.add(badge);
      var note = new Span("Record: public | Files: restricted");
      note.addClassName("extra-small-body-text");
      note.addClassName("color-secondary");
      wrapper.add(note);
    }
    return wrapper;
  }

  private void addDetailRow(Div container, String label, String value) {
    var row = new Div();
    row.addClassNames("flex-horizontal", "gap-02");
    row.getStyle().set("align-items", "baseline");
    var labelSpan = new Span(label + ":");
    labelSpan.addClassName("extra-small-body-text");
    labelSpan.getStyle().set("font-weight", "600");
    labelSpan.getStyle().set("min-width", "140px");
    labelSpan.addClassName("color-secondary");
    var valueSpan = new Span(value);
    valueSpan.addClassName("normal-body-text");
    row.add(labelSpan, valueSpan);
    container.add(row);
  }

  private void updateResourceCount(Span countBadge) {
    int total = connectedResources.size();
    long publicCount = connectedResources.stream()
        .filter(r -> r.accessLevel() == AccessLevel.PUBLIC).count();
    long restrictedCount = total - publicCount;
    countBadge.setText(
        "%d resource(s) — %d public, %d restricted".formatted(total, publicCount, restrictedCount));
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CONNECT SIDEBAR (replaces modal — slides in from right)
  // ══════════════════════════════════════════════════════════════════════

  // ── Sidebar CSS class names managed via getStyle() ──────────────
  private static final String SIDEBAR_OPEN_CSS =
      "position:fixed;top:0;right:0;width:640px;height:100%;"
          + "background-color:var(--lumo-base-color);"
          + "box-shadow:-4px 0 24px rgba(0,0,0,0.12);"
          + "z-index:1000;display:none;box-sizing:border-box;"
          + "overflow-y:auto;";

  private void buildConnectSidebar() {
    // Overlay (semi-transparent backdrop)
    sidebarOverlay = new Div();
    sidebarOverlay.getStyle().set("position", "fixed");
    sidebarOverlay.getStyle().set("top", "0");
    sidebarOverlay.getStyle().set("left", "0");
    sidebarOverlay.getStyle().set("width", "100%");
    sidebarOverlay.getStyle().set("height", "100%");
    sidebarOverlay.getStyle().set("background-color", "rgba(0,0,0,0.3)");
    sidebarOverlay.getStyle().set("z-index", "999");
    sidebarOverlay.getStyle().set("display", "none");
    sidebarOverlay.addClickListener(e -> closeConnectSidebar());

    // Sidebar panel — initially hidden (display:none), shown via openConnectSidebar()
    sidebarPanel = new Div();
    sidebarPanel.getStyle().set("position", "fixed");
    sidebarPanel.getStyle().set("top", "0");
    sidebarPanel.getStyle().set("right", "0");
    sidebarPanel.getStyle().set("width", "640px");
    sidebarPanel.getStyle().set("height", "100%");
    sidebarPanel.getStyle().set("background-color", "var(--lumo-base-color)");
    sidebarPanel.getStyle().set("box-shadow", "-4px 0 24px rgba(0,0,0,0.12)");
    sidebarPanel.getStyle().set("z-index", "1000");
    sidebarPanel.getStyle().set("box-sizing", "border-box");
    sidebarPanel.getStyle().set("overflow-y", "auto");
    sidebarPanel.getStyle().set("display", "none");

    // ── Sidebar container (flex layout inside the panel) ─────────
    var sidebarBody = new Div();
    // Use explicit CSS instead of flex-vertical to avoid conflicts with fixed positioning
    sidebarBody.getStyle().set("height", "100%");
    sidebarBody.getStyle().set("box-sizing", "border-box");
    sidebarBody.getStyle().set("display", "flex");
    sidebarBody.getStyle().set("flex-direction", "column");

    // ── Header ────────────────────────────────────────────────────
    var sidebarHeader = new Div();
    sidebarHeader.addClassNames("flex-horizontal", "items-center");
    sidebarHeader.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    sidebarHeader.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
    sidebarHeader.getStyle().set("flex-shrink", "0");
    sidebarHeader.getStyle().set("gap", "var(--lumo-space-s)");

    var sidebarTitle = new Span("Connect Datasets");
    sidebarTitle.addClassName("heading-3");
    sidebarTitle.getStyle().set("flex-grow", "1");

    var closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    closeButton.setTooltipText("Close");
    closeButton.addClickListener(e -> closeConnectSidebar());

    sidebarHeader.add(sidebarTitle, closeButton);
    sidebarBody.add(sidebarHeader);

    // ─ Content area (scrollable) ────────────────────────────────
    var contentArea = new Div();
    contentArea.getStyle().set("flex-grow", "1");
    contentArea.getStyle().set("overflow-y", "auto");
    contentArea.getStyle().set("padding", "var(--lumo-space-l)");

    // Compact credential notice (dismissible, below search form)
    contentArea.add(buildCompactCredentialNotice());

    // Info note
    var infoNote = new InfoBox()
        .setInfoText(
            "Search for datasets on InvenioRDM repositories and connect them to this project. "
                + "Both public and restricted datasets can be found here — access level is shown inline.")
        .setClosable(true);
    infoNote.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    contentArea.add(infoNote);

    // Search bar (compact horizontal layout)
    var searchForm = new Div();
    searchForm.getStyle().set("margin-bottom", "var(--lumo-space-l)");

    instanceSelector.setItems(INVENIO_INSTANCES);
    instanceSelector.setPlaceholder("Select repository…");
    instanceSelector.setValue(INVENIO_INSTANCES.get(0));
    instanceSelector.setWidth("180px");
    instanceSelector.setLabel("Repository");
    instanceSelector.getStyle().set("flex-shrink", "0");
    // Tag the overlay so we can raise its z-index via CSS (the overlay is
    // teleported to <body> at z-index 200, which sits below the sidebar)
    instanceSelector.setOverlayClassName("connect-dataset-sidebar-overlay");
    // Auto-update search results when repository selection changes
    instanceSelector.addValueChangeListener(e -> performSidebarSearch());

    searchField.setPlaceholder("Search by title, DOI, or creator…");
    searchField.setClearButtonVisible(true);
    // Key trick: min-width: 0 allows flex children with intrinsic width to shrink
    searchField.getStyle().set("min-width", "0");
    searchField.getStyle().set("flex-shrink", "1");
    searchField.getStyle().set("flex-grow", "1");

    var searchRow = new Div();
    searchRow.getStyle().set("display", "flex");
    searchRow.getStyle().set("gap", "var(--lumo-space-xs)");
    searchRow.getStyle().set("align-items", "flex-end");
    searchRow.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    searchRow.getStyle().set("min-width", "0");
    searchRow.add(instanceSelector, searchField);

    var searchButtonBar = new Div();
    searchButtonBar.getStyle().set("display", "flex");
    searchButtonBar.getStyle().set("gap", "var(--lumo-space-xs)");
    searchButtonBar.getStyle().set("overflow", "hidden");

    var searchButton = new Button("Search", VaadinIcon.SEARCH.create());
    searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    searchButton.addClickListener(e -> performSidebarSearch());

    // Trigger search when user presses Enter in the search field
    searchField.addKeyDownListener(Key.ENTER, e -> performSidebarSearch());

    var clearButton = new Button("Clear");
    clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    clearButton.addClickListener(e -> {
      searchField.clear();
      performSidebarSearch();
    });

    searchButtonBar.add(searchButton, clearButton);
    searchForm.add(searchRow, searchButtonBar);
    contentArea.add(searchForm);

    // ── Search results as card list (not a multi-column grid) ────
    // Using a Vaadin Grid with a single column that renders a full card per item.
    // This avoids column-width issues in narrow containers.
    searchResultsGrid = new Grid<>();
    searchResultsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_ROW_BORDERS);
    searchResultsGrid.setSelectionMode(SelectionMode.MULTI);
    searchResultsGrid.setWidthFull();
    searchResultsGrid.setAllRowsVisible(true);

    searchResultsGrid.addComponentColumn(resource -> buildSearchResultCard(resource))
        .setFlexGrow(1)
        .setKey("card");

    searchResultsGrid.addSelectionListener(event -> {
      int count = event.getAllSelectedItems().size();
      selectionCountLabel.setText(
          count == 0 ? "" : count + " selected");
      sidebarConnectBtn.setEnabled(count > 0);
    });

    contentArea.add(searchResultsGrid);
    sidebarBody.add(contentArea);

    // ── Footer: Connect button ────────────────────────────────────
    var sidebarFooter = new Div();
    sidebarFooter.addClassNames("flex-horizontal", "items-center");
    sidebarFooter.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    sidebarFooter.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
    sidebarFooter.getStyle().set("flex-shrink", "0");
    sidebarFooter.getStyle().set("gap", "var(--lumo-space-s)");

    selectionCountLabel = new Span("");
    selectionCountLabel.addClassName("normal-body-text");
    selectionCountLabel.addClassName("color-secondary");
    selectionCountLabel.getStyle().set("flex-grow", "1");

    sidebarConnectBtn = new Button("Connect Selected", VaadinIcon.PLUS_CIRCLE.create());
    sidebarConnectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sidebarConnectBtn.setEnabled(false);
    sidebarConnectBtn.addClickListener(e -> connectSelectedFromSidebar());

    sidebarFooter.add(selectionCountLabel, sidebarConnectBtn);
    sidebarBody.add(sidebarFooter);

    // Assemble sidebar
    sidebarPanel.add(sidebarBody);
    add(sidebarOverlay, sidebarPanel);
  }

  /**
   * Builds a single card-style row for the sidebar search results.
   * Shows title, creator, DOI, access badge, provider, and date stacked
   * — all in a single grid column to avoid width issues in narrow containers.
   */
  private Div buildSearchResultCard(SearchableResource resource) {
    var card = new Div();
    card.addClassName("border");
    card.addClassName("rounded-02");
    card.getStyle().set("padding", "var(--lumo-space-m)");
    card.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    card.getStyle().set("cursor", "pointer");

    // Top row: access badge + provider tag + date
    var topRow = new Div();
    topRow.addClassNames("flex-horizontal", "items-center", "gap-02");
    topRow.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

    Tag accessBadge;
    if (resource.accessLevel() == AccessLevel.PUBLIC) {
      accessBadge = new Tag("Public");
      accessBadge.setTagColor(TagColor.SUCCESS);
    } else {
      accessBadge = new Tag("Restricted");
      accessBadge.setTagColor(TagColor.WARNING);
    }
    topRow.add(accessBadge);

    var providerTag = new Tag(resource.resourceProvider());
    providerTag.setTagColor("Zenodo".equals(resource.resourceProvider())
        ? TagColor.PRIMARY : TagColor.TEAL);
    topRow.add(providerTag);

    var dateSpan = new Span(resource.publicationDate().format(
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
    dateSpan.addClassName("extra-small-body-text");
    dateSpan.addClassName("color-secondary");
    dateSpan.getStyle().set("margin-left", "auto");
    topRow.add(dateSpan);

    card.add(topRow);

    // Title
    var titleSpan = new Span(resource.title());
    titleSpan.addClassName("normal-body-text");
    titleSpan.getStyle().set("font-weight", "600");
    titleSpan.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
    titleSpan.getStyle().set("display", "block");
    card.add(titleSpan);

    // Creator + DOI row
    var metaRow = new Div();
    metaRow.addClassNames("flex-horizontal", "gap-04", "items-center");
    var creatorSpan = new Span("by " + resource.creator());
    creatorSpan.addClassName("extra-small-body-text");
    creatorSpan.addClassName("color-secondary");
    metaRow.add(creatorSpan);

    var doiAnchor = new Anchor("https://doi.org/" + resource.pid(), resource.pid());
    doiAnchor.setTarget(AnchorTarget.BLANK);
    doiAnchor.addClassName("extra-small-body-text");
    metaRow.add(doiAnchor);

    card.add(metaRow);

    return card;
  }

  /**
   * Builds a compact, collapsible credential notice that sits below the search form.
   * Minimal visual impact — just a dismissible inline notice with an optional expand for details.
   */
  private Div buildCompactCredentialNotice() {
    var notice = new Div();
    notice.addClassNames("border", "rounded-02");
    notice.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
    notice.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    notice.getStyle().set("background-color", "var(--lumo-contrast-5pct)");

    var header = new Div();
    header.addClassNames("flex-horizontal", "gap-02", "items-center");

    var infoIcon = VaadinIcon.INFO.create();
    infoIcon.getStyle().set("color", "var(--lumo-primary-color)");
    infoIcon.getStyle().set("font-size", "var(--lumo-icon-size-s)");

    var text = new Span("Repository access requires credentials for restricted datasets.");
    text.addClassName("extra-small-body-text");
    text.getStyle().set("flex-grow", "1");

    header.add(infoIcon, text);
    notice.add(header);

    // Expandable details section
    var detailsSection = new Div();
    detailsSection.getStyle().set("display", "none");
    detailsSection.getStyle().set("margin-top", "var(--lumo-space-s)");
    detailsSection.getStyle().set("padding-top", "var(--lumo-space-s)");
    detailsSection.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");

    var expandedText = new Span(
        "Public datasets work without credentials. For restricted datasets, "
            + "you need a Personal Access Token from your repository account.");
    expandedText.addClassName("extra-small-body-text");
    expandedText.getStyle().set("display", "block");
    expandedText.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

    var actions = new Div();
    actions.addClassNames("flex-horizontal", "gap-02");

    var setupLink = new Anchor("#/account/tokens", "Set up access \u2192");
    setupLink.addClassName("extra-small-body-text");
    setupLink.getStyle().set("color", "var(--lumo-primary-color)");
    setupLink.getStyle().set("text-decoration", "none");
    setupLink.getStyle().set("font-weight", "500");

    var learnMoreLink = new Button("What is a token?");
    learnMoreLink.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    learnMoreLink.addClickListener(e -> showCredentialsExplanation());

    actions.add(setupLink, learnMoreLink);
    detailsSection.add(expandedText, actions);

    // Toggle button
    var toggleBtn = new Button("Details");
    toggleBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    boolean[] expanded = {false};
    toggleBtn.addClickListener(e -> {
      expanded[0] = !expanded[0];
      detailsSection.getStyle().set("display", expanded[0] ? "block" : "none");
      toggleBtn.setText(expanded[0] ? "Less" : "Details");
    });

    // Dismiss button
    var dismissBtn = new Button(VaadinIcon.CLOSE_SMALL.create());
    dismissBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    dismissBtn.addClickListener(e -> notice.setVisible(false));

    header.add(toggleBtn, dismissBtn);
    notice.add(detailsSection);

    return notice;
  }

  /**
   * Shows an explanation dialog about what repository credentials are
   * and how to set them up. Uses plain language aimed at non-technical users.
   */
  private void showCredentialsExplanation() {
    var dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("What are repository credentials?");
    dialog.setWidth("480px");

    var content = new Div();
    content.addClassNames("flex-vertical", "gap-03");
    content.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");

    var intro = new Div();
    intro.getElement().setProperty("innerHTML",
        "Repositories like Zenodo and FDAT require authentication "
            + "to access datasets that are not publicly visible. You can grant "
            + "access by generating a <b>Personal Access Token</b> \u2014 a secret "
            + "key that identifies you to the repository.");
    intro.addClassName("normal-body-text");
    content.add(intro);

    // Steps
    var stepsTitle = new Span("How to set up access:");
    stepsTitle.addClassName("normal-body-text");
    stepsTitle.getStyle().set("font-weight", "600");
    content.add(stepsTitle);

    var steps = new Div();
    steps.getElement().setProperty("innerHTML",
        "<ol style='margin:0;padding-left:1.2em;'>"
            + "<li style='margin-bottom:var(--lumo-space-xs);'>"
            + "Go to your <b>profile</b> \u2192 <b>Personal access tokens</b></li>"
            + "<li style='margin-bottom:var(--lumo-space-xs);'>"
            + "Click <b>Create new token</b></li>"
            + "<li style='margin-bottom:var(--lumo-space-xs);'>"
            + "Give it a name (e.g. <i>'Data Manager'</i>) and select <b>read</b> permissions</li>"
            + "<li style='margin-bottom:var(--lumo-space-xs);'>"
            + "Copy the token and save it securely</li>"
            + "</ol>");
    content.add(steps);

    // Security note
    var securityNote = new Span(
        "\uD83D\uDD12 Your token is stored securely in the system vault and is only "
            + "used to search repositories on your behalf. It never leaves the platform.");
    securityNote.addClassName("extra-small-body-text");
    securityNote.addClassName("color-secondary");
    securityNote.getStyle().set("margin-top", "var(--lumo-space-xs)");
    securityNote.getStyle().set("display", "block");
    content.add(securityNote);

    dialog.add(content);

    // Footer buttons
    var goBtn = new Button("Go to token settings \u2192", e -> {
      dialog.close();
      UI.getCurrent().getPage().setLocation("#/account/tokens");
    });
    goBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    var closeBtn = new Button("Close", e -> dialog.close());
    closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(closeBtn, goBtn);

    // Raise the dialog above the fixed-position sidebar (z-index: 1000).
    // Vaadin teleports dialog overlays to <body> with a default z-index of 200,
    // which renders behind the sidebar. We add a CSS class to the overlay that
    // sets z-index: 1001 via dialog.css in the theme.
    dialog.getClassNames().add("connect-dataset-sidebar-dialog");

    dialog.open();
  }

  private void openConnectSidebar() {
    sidebarOpen = true;
    sidebarOverlay.getStyle().set("display", "block");
    sidebarPanel.getStyle().set("display", "block");
    // Populate search results lazily (Grid measures width when visible),
    // filtered by the currently selected repository
    performSidebarSearch();
  }

  private void closeConnectSidebar() {
    sidebarOpen = false;
    sidebarOverlay.getStyle().set("display", "none");
    sidebarPanel.getStyle().set("display", "none");
    searchResultsGrid.deselectAll();
    selectionCountLabel.setText("");
    sidebarConnectBtn.setEnabled(false);
  }

  // ══════════════════════════════════════════════════════════════════════
  //  GRID DATA & FILTERING
  // ══════════════════════════════════════════════════════════════════════

  private void refreshResourcesGrid() {
    String filter = accessFilter.getValue();
    List<ConnectedResource> filtered;
    if ("Public".equals(filter)) {
      filtered = connectedResources.stream()
          .filter(r -> r.accessLevel() == AccessLevel.PUBLIC)
          .toList();
    } else if ("Restricted".equals(filter)) {
      filtered = connectedResources.stream()
          .filter(r -> r.accessLevel() == AccessLevel.RESTRICTED)
          .toList();
    } else {
      filtered = new ArrayList<>(connectedResources);
    }
    resourcesGrid.setItems(filtered);
  }

  private void performSidebarSearch() {
    String selectedRepo = instanceSelector.getValue();
    String term = searchField.getValue();

    // Filter by selected repository first
    var repoFiltered = MOCK_SEARCH_RESULTS.stream()
        .filter(r -> matchesRepository(r, selectedRepo))
        .toList();

    if (term == null || term.isBlank()) {
      searchResultsGrid.setItems(new ArrayList<>(repoFiltered));
      return;
    }
    String lower = term.toLowerCase();
    var results = repoFiltered.stream()
        .filter(r -> r.title().toLowerCase().contains(lower)
            || r.pid().toLowerCase().contains(lower)
            || r.creator().toLowerCase().contains(lower)
            || r.description().toLowerCase().contains(lower))
        .toList();
    searchResultsGrid.setItems(results);
  }

  /**
   * Checks whether a searchable resource belongs to the selected repository.
   */
  private boolean matchesRepository(SearchableResource resource, String repository) {
    if (repository == null || repository.isBlank()) {
      return true;
    }
    return resource.resourceProvider().equals(extractProviderName(repository));
  }

  /**
   * Extracts the provider display name from a repository selector entry.
   * E.g. "Zenodo (zenodo.org)" → "Zenodo"
   */
  private String extractProviderName(String repositoryEntry) {
    int idx = repositoryEntry.indexOf(' ');
    if (idx > 0) {
      return repositoryEntry.substring(0, idx);
    }
    return repositoryEntry;
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CONNECT / REMOVE / SYNC ACTIONS
  // ══════════════════════════════════════════════════════════════════════

  private void connectSelectedFromSidebar() {
    var selected = searchResultsGrid.getSelectedItems();
    if (selected.isEmpty()) {
      return;
    }
    for (SearchableResource sr : selected) {
      connectedResources.add(new ConnectedResource(
          sr.title(), sr.pid(), sr.accessLevel(), sr.version(),
          sr.accessLink(), sr.publicationDate(),
          "Current User (demo)", sr.resourceProvider(), sr.creator(),
          sr.resourceType(), sr.community(),
          null, // linked experiment — not set at connect time
          sr.id(), LocalDate.now(), false
      ));
    }
    refreshResourcesGrid();
    searchResultsGrid.deselectAll();
    closeConnectSidebar();
    showSuccessNotification(selected.size()
        + " dataset(s) connected to this project.");
  }

  private void confirmRemoveResource(ConnectedResource resource) {
    // Use a lightweight approach — in a real app, use the project's dialog pattern
    var dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setCloseOnOutsideClick(false);
    dialog.setCloseOnEsc(false);
    dialog.setWidth("480px");

    var body = new Div();
    body.addClassNames("flex-vertical", "gap-03", "padding-horizontal-05",
        "padding-vertical-04");
    var headerRow = new Div();
    headerRow.addClassNames("flex-horizontal", "gap-03");
    var icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
    icon.addClassName("icon-color-warning");
    var titleSpan = new Span("Remove Dataset Connection");
    titleSpan.addClassName("heading-3");
    headerRow.add(icon, titleSpan);
    body.add(headerRow);

    body.add(new Span("Are you sure you want to remove the connection to:"));
    var dsLabel = new Span(resource.title());
    dsLabel.getStyle().set("font-weight", "600");
    body.add(dsLabel);
    body.add(new Span("This will not delete the dataset on " + resource.resourceProvider()
        + " — it only removes the link from this project."));

    var footer = new Div();
    footer.addClassNames("flex-horizontal", "gap-03", "padding-horizontal-05",
        "padding-vertical-03");
    footer.getStyle().set("justify-content", "flex-end");
    var cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    var removeBtn = new Button("Remove", VaadinIcon.TRASH.create(), e -> {
      connectedResources.removeIf(r -> r.id().equals(resource.id()));
      refreshResourcesGrid();
      dialog.close();
      showSuccessNotification("Connection removed: " + resource.title());
    });
    removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
    footer.add(cancelBtn, removeBtn);

    dialog.add(body);
    dialog.getFooter().add(footer);
    dialog.open();
  }

  private void syncSingleResource(ConnectedResource resource) {
    showInfoNotification("Syncing '" + resource.title() + "' with "
        + resource.resourceProvider() + "…");
    var ui = UI.getCurrent();
    new Thread(() -> {
      try {
        Thread.sleep(1500);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      ui.access(() -> {
        showInfoNotification("Sync complete for '" + resource.title() + "'.");
      });
    }).start();
  }

  private void syncAllResources() {
    if (connectedResources.isEmpty()) {
      showInfoNotification("No resources connected to sync.");
      return;
    }
    showInfoNotification("Syncing all " + connectedResources.size() + " resources…");
    var ui = UI.getCurrent();
    new Thread(() -> {
      try {
        Thread.sleep(2000);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      ui.access(() -> {
        showSuccessNotification("All " + connectedResources.size()
            + " resources are up to date.");
      });
    }).start();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  NOTIFICATIONS
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

  // ══════════════════════════════════════════════════════════════════════
  //  SEED DATA
  // ══════════════════════════════════════════════════════════════════════

  private void seedConnectedResources() {
    // Public resources
    connectedResources.add(new ConnectedResource(
        "Benchmark dataset for reproducibility assessment",
        "10.5281/zenodo.9999001", AccessLevel.PUBLIC,
        "v2.0", "https://zenodo.org/records/9999001",
        LocalDate.of(2024, 6, 10),
        "Alice Schmidt", "Zenodo", "Alice Schmidt",
        "Dataset", null,
        "Explorative analysis of heat regulative proteins",
        "seed-01", LocalDate.of(2025, 1, 15), false));

    connectedResources.add(new ConnectedResource(
        "Metabolomics reference spectra library",
        "10.5281/zenodo.9999002", AccessLevel.PUBLIC,
        "v1.2", "https://zenodo.org/records/9999002",
        LocalDate.of(2024, 9, 22),
        "Bob Fernandez", "Zenodo", "Bob Fernandez",
        "Dataset", "QBiC",
        "Metabolomics reference for pathway analysis",
        "seed-02", LocalDate.of(2025, 3, 22), true));

    connectedResources.add(new ConnectedResource(
        "Spatial transcriptomics atlas of human kidney development",
        "10.5281/fdat.9876544", AccessLevel.PUBLIC,
        "v1", "https://fdat.uni-tuebingen.de/records/9876544",
        LocalDate.of(2025, 4, 12),
        "Carol Yamamoto", "FDAT", "S. Yamamoto, F. Rossi, QBiC",
        "Dataset", "CMFI",
        null,
        "seed-03", LocalDate.of(2025, 5, 1), false));

    // Restricted resources
    connectedResources.add(new ConnectedResource(
        "Multi-center clinical proteomics study (Controlled)",
        "10.5281/fdat.7777001", AccessLevel.RESTRICTED,
        "v1.0", "https://fdat.uni-tuebingen.de/records/7777001",
        LocalDate.of(2025, 2, 18),
        "Carol Yamamoto", "FDAT", "QBiC Clinical Collaboration",
        "Dataset", "SFB209",
        "Clinical proteomics — explorative biomarker study",
        "seed-04", LocalDate.of(2025, 5, 8), false));

    connectedResources.add(new ConnectedResource(
        "Clinical metabolomics data — Cohort A (embargo until 2026-12)",
        "10.5281/zenodo.2345601", AccessLevel.RESTRICTED,
        "v1", "https://zenodo.org/records/2345601",
        LocalDate.of(2025, 6, 1),
        "Current User", "Zenodo", "R. Schmidt, M. Bauer",
        "Dataset", null,
        null,
        "seed-05", LocalDate.of(2025, 6, 20), false));
  }
}
