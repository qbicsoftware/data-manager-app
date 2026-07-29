package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 * <p><b>Stories 14 & 15 (credential management):</b> A dedicated section below the
 * connected resources grid allows users to configure InvenioRDM instances by adding or
 * removing Personal Access Tokens. Configuring a token unlocks access to restricted
 * datasets on that instance; removing it revokes that access. Token validation is
 * simulated in this prototype.</p>
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

  // ── InvenioRDM instance definitions & credential state ────────────────

  /**
   * An InvenioRDM instance available for credential configuration.
   * Users can store a Personal Access Token for each instance to access
   * restricted datasets.
   */
  record InvenioRDMInstance(
      String id,
      String displayName,
      String shortName,
      String baseUrl,
      String description,
      String tokenSetupUrl
  ) {}

  /**
   * A stored credential (Personal Access Token) for an InvenioRDM instance.
   * The actual token value is never held in memory as plaintext in the UI;
   * only a masked representation is displayed.
   */
  record RepositoryCredential(
      String instanceId,
      String maskedToken,
      LocalDate addedOn
  ) {}

  /**
   * InvenioRDM instances that users can configure with credentials.
   */
  private static final List<InvenioRDMInstance> AVAILABLE_INSTANCES = List.of(
      new InvenioRDMInstance(
          "zenodo",
          "Zenodo (zenodo.org)",
          "Zenodo",
          "https://zenodo.org",
          "Open-access research data repository operated by CERN. Widely accepted "
              + "by journals and funders; any researcher can deposit datasets and "
              + "receive a DOI.",
          "https://zenodo.org/account/settings/applications/tokens/new/"
      ),
      new InvenioRDMInstance(
          "fdat",
          "FDAT (fdat.uni-tuebingen.de)",
          "FDAT",
          "https://fdat.uni-tuebingen.de",
          "InvenioRDM instance operated by the University of Tübingen. Provides "
              + "FAIR-compliant publishing for institutional and collaborative "
              + "research datasets.",
          "https://fdat.uni-tuebingen.de/account/settings/applications/tokens/new/"
      )
  );

  // ── Mutable state ─────────────────────────────────────────────────────

  private final List<ConnectedResource> connectedResources = new ArrayList<>();

  /**
   * Maps InvenioRDM instance IDs (e.g. "zenodo") to their stored credentials.
   * An entry present in this map means the instance is fully configured and
   * the user can search restricted datasets on it.
   */
  private final Map<String, RepositoryCredential> configuredCredentials = new HashMap<>();

  /** Content container for the credentials section — rebuildable when state changes. */
  private Div credentialsContentContainer;

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

    // ── Seed credentials BEFORE building the section so the cards
    //     render in their correct configured / unconfigured state.
    //     Zenodo is pre-configured; FDAT is intentionally left empty
    //     so the demo shows both card states side-by-side.
    seedCredentials();

    // ── Main content: Connected Resources section ──────────────────
    add(buildConnectedResourcesSection());

    // ── Repository credentials section (stories 14 & 15) ─────────
    add(buildRepositoryCredentialsSection());

    // ── Sidebar overlay + panel (initially hidden) ────────────────
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

  // ══════════════════════════════════════════════════════════════════════
  //  REPOSITORY CREDENTIALS — Account Settings (Stories 14 & 15)
  // ══════════════════════════════════════════════════════════════════════

  /**
   * Builds the "Repository Access" section that lets users configure
   * InvenioRDM instances with Personal Access Tokens (Story 14) and remove
   * them again (Story 15). Each available instance is shown as a card with
   * its connection status and an appropriate action button.
   */
  private Section buildRepositoryCredentialsSection() {
    var section = new Section.SectionBuilder().build();

    var header = new SectionHeader(
        new SectionTitle("Repository Access — Account Settings"),
        new ActionBar(),
        new SectionNote(
            "Connect your personal access tokens for InvenioRDM repositories. "
                + "A valid token enables Data Manager to search and connect "
                + "access-restricted datasets from that repository to your projects. "
                + "Public datasets are always accessible without a token.")
    );
    header.enableControls();
    section.setHeader(header);

    var content = new SectionContent();

    credentialsContentContainer = new Div();
    credentialsContentContainer.addClassNames("flex-vertical", "gap-03");
    refreshCredentialsContent();
    content.add(credentialsContentContainer);

    section.setContent(content);
    return section;
  }

  /**
   * Rebuilds the cards inside {@link #credentialsContentContainer} to reflect
   * the current state of {@link #configuredCredentials}. Called after tokens
   * are added or removed.
   */
  private void refreshCredentialsContent() {
    credentialsContentContainer.removeAll();

    // Benefits description — shown once at the top of the section
    var benefitsCallout = new Div();
    benefitsCallout.addClassNames("border", "rounded-02");
    benefitsCallout.getStyle().set("padding",
        "var(--lumo-space-m) var(--lumo-space-l)");
    benefitsCallout.getStyle().set("background-color",
        "var(--lumo-primary-color-10pct)");
    benefitsCallout.getStyle().set("border-color",
        "var(--lumo-primary-color-30pct)");
    benefitsCallout.getStyle().set("margin-bottom", "var(--lumo-space-s)");

    var benefitsHeader = new Div();
    benefitsHeader.addClassNames("flex-horizontal", "gap-02", "items-center");
    var benefitsIcon = VaadinIcon.LIGHTBULB.create();
    benefitsIcon.getStyle().set("color", "var(--lumo-primary-color)");
    var benefitsTitle = new Span(
        "Why configure repository access?");
    benefitsTitle.getStyle().set("font-weight", "600");
    benefitsTitle.addClassName("normal-body-text");
    benefitsHeader.add(benefitsIcon, benefitsTitle);
    benefitsCallout.add(benefitsHeader);

    var benefitsList = new Div();
    benefitsList.getStyle().set("margin-top", "var(--lumo-space-xs)");
    benefitsList.getStyle().set("padding-left", "var(--lumo-space-l)");
    benefitsList.getElement().setProperty("innerHTML",
        "<ul style='margin:0;padding-left:1.2em;'>"
            + "<li style='margin-bottom:var(--lumo-space-xxs);'>"
            + "Search and connect <b>access-restricted datasets</b> "
            + "to your projects (e.g. datasets under embargo or with controlled access)</li>"
            + "<li style='margin-bottom:var(--lumo-space-xxs);'>"
            + "Keep an overview of <b>all associated data</b> — public and restricted — "
            + "in a single place</li>"
            + "<li style='margin-bottom:var(--lumo-space-xxs);'>"
            + "Collaborate with your team on datasets that are not yet publicly visible</li>"
            + "<li>Your token is stored <b>encrypted in the system vault</b> and is only "
            + "used to query the repository on your behalf.</li>"
            + "</ul>");
    benefitsList.addClassName("normal-body-text");
    benefitsCallout.add(benefitsList);
    credentialsContentContainer.add(benefitsCallout);

    // One card per InvenioRDM instance
    for (var instance : AVAILABLE_INSTANCES) {
      credentialsContentContainer.add(buildInstanceCard(instance));
    }
  }

  /**
   * Builds a card for a single InvenioRDM instance showing its connection status
   * and the appropriate action button (Configure or Remove).
   */
  private Div buildInstanceCard(InvenioRDMInstance instance) {
    var card = new Div();
    card.addClassNames("border", "rounded-02");
    card.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");

    boolean isConfigured = configuredCredentials.containsKey(instance.id());

    // ── Top row: icon + name + status badge ───────────────────────
    var topRow = new Div();
    topRow.addClassNames("flex-horizontal", "items-center", "gap-03");
    topRow.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

    var repoIcon = VaadinIcon.CLOUD.create();
    repoIcon.getStyle().set("font-size", "var(--lumo-icon-size-m)");
    repoIcon.getStyle().set("color",
        "Zenodo".equals(instance.shortName())
            ? "var(--lumo-primary-color)"
            : "var(--lumo-success-color)");
    topRow.add(repoIcon);

    var nameAndDesc = new Div();
    nameAndDesc.addClassNames("flex-vertical", "gap-01");
    nameAndDesc.getStyle().set("flex-grow", "1");

    var nameSpan = new Span(instance.displayName());
    nameSpan.getStyle().set("font-weight", "600");
    nameSpan.addClassName("normal-body-text");
    nameAndDesc.add(nameSpan);
    topRow.add(nameAndDesc);

    // Status badge
    if (isConfigured) {
      var connectedBadge = new Tag("\u2713 Connected");
      connectedBadge.setTagColor(TagColor.SUCCESS);
      topRow.add(connectedBadge);
    } else {
      var notConfiguredBadge = new Tag("Not configured");
      notConfiguredBadge.setTagColor(TagColor.CONTRAST);
      topRow.add(notConfiguredBadge);
    }
    card.add(topRow);

    // ── Description ────────────────────────────────────────────────
    var descSpan = new Span(instance.description());
    descSpan.addClassName("extra-small-body-text");
    descSpan.addClassName("color-secondary");
    descSpan.getStyle().set("display", "block");
    descSpan.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    card.add(descSpan);

    // ── Bottom row: details (if configured) + action button ────────
    var bottomRow = new Div();
    bottomRow.addClassNames("flex-horizontal", "items-center");
    bottomRow.getStyle().set("gap", "var(--lumo-space-s)");

    if (isConfigured) {
      RepositoryCredential credential = configuredCredentials.get(instance.id());

      var detailsWrapper = new Div();
      detailsWrapper.addClassNames("flex-horizontal", "gap-04", "items-center");
      detailsWrapper.getStyle().set("flex-grow", "1");

      var tokenInfo = new Span(
          "Token: " + credential.maskedToken()
              + "  |  Added: "
              + credential.addedOn().format(
                  DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
      tokenInfo.addClassName("extra-small-body-text");
      tokenInfo.addClassName("color-secondary");
      detailsWrapper.add(tokenInfo);

      var removeButton = new Button("Remove",
          VaadinIcon.TRASH.create());
      removeButton.addThemeVariants(
          ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
      removeButton.setTooltipText(
          "Remove the stored token for " + instance.shortName()
              + ". Restricted datasets from this instance will no longer "
              + "be accessible.");
      removeButton.addClickListener(
          e -> openRemoveTokenDialog(instance));
      bottomRow.add(detailsWrapper, removeButton);
    } else {
      var hintText = new Span(
          "No personal access token configured for this repository.");
      hintText.addClassName("extra-small-body-text");
      hintText.addClassName("color-secondary");
      hintText.getStyle().set("flex-grow", "1");

      var configureButton = new Button("Configure",
          VaadinIcon.PLUS_CIRCLE.create());
      configureButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      configureButton.setTooltipText(
          "Add a personal access token to connect restricted datasets from "
              + instance.shortName() + ".");
      configureButton.addClickListener(
          e -> openAddTokenDialog(instance));
      bottomRow.add(hintText, configureButton);
    }
    card.add(bottomRow);

    return card;
  }

  // ── Add Token Dialog (Story 14) ────────────────────────────────────────

  /**
   * Opens a dialog that lets the user add a Personal Access Token for the
   * given InvenioRDM instance. Token validation is simulated in this
   * prototype — any non-empty token is accepted.
   */
  private void openAddTokenDialog(InvenioRDMInstance instance) {
    var dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Connect " + instance.displayName());
    dialog.setWidth("520px");
    dialog.setCloseOnOutsideClick(false);

    var content = new Div();
    content.addClassNames("flex-vertical", "gap-03");
    content.getStyle().set("padding",
        "var(--lumo-space-s) var(--lumo-space-m)");

    // Explanation
    var introText = new Div();
    introText.addClassName("normal-body-text");
    introText.getElement().setProperty("innerHTML",
        "To connect access-restricted datasets from <b>" + instance.shortName()
            + "</b>, provide a Personal Access Token from your "
            + instance.shortName() + " account. The token is validated against "
            + "the repository and stored securely.");
    content.add(introText);

    // Token input
    var tokenField = new PasswordField();
    tokenField.setLabel("Personal Access Token");
    tokenField.setPlaceholder("Paste your token here…");
    tokenField.setWidthFull();
    tokenField.setRequiredIndicatorVisible(true);
    tokenField.setHelperText(
        "Generate a token in your " + instance.shortName()
            + " account settings.");
    content.add(tokenField);

    // Helper link to the token creation page
    var tokenSetupLink = new Anchor(instance.tokenSetupUrl(),
        "Open " + instance.shortName() + " token settings \u2192");
    tokenSetupLink.setTarget(AnchorTarget.BLANK);
    tokenSetupLink.addClassName("extra-small-body-text");
    content.add(tokenSetupLink);

    // Token status feedback area
    var statusArea = new Div();
    statusArea.getStyle().set("display", "none");
    statusArea.getStyle().set("padding", "var(--lumo-space-s)");
    statusArea.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    content.add(statusArea);

    // Validation hint
    var validationHint = new Div();
    validationHint.addClassName("extra-small-body-text");
    validationHint.addClassName("color-secondary");
    validationHint.getStyle().set("font-style", "italic");
    validationHint.setText(
        "In this prototype, any non-empty token is accepted. "
            + "In production, the token is validated against the "
            + "InvenioRDM REST API.");
    content.add(validationHint);

    // Security note
    var securityNote = new Div();
    securityNote.addClassNames("flex-horizontal", "gap-02");
    securityNote.getStyle().set("padding",
        "var(--lumo-space-xs) var(--lumo-space-s)");
    securityNote.getStyle().set("background-color",
        "var(--lumo-contrast-5pct)");
    securityNote.getStyle().set("border-radius",
        "var(--lumo-border-radius-m)");
    var lockIcon = VaadinIcon.LOCK.create();
    lockIcon.getStyle().set("color", "var(--lumo-success-color)");
    lockIcon.getStyle().set("font-size", "var(--lumo-icon-size-s)");
    var lockText = new Span(
        "Your token is stored encrypted in the system vault. "
            + "It never leaves the platform and is only used to query "
            + instance.shortName() + " on your behalf.");
    lockText.addClassName("extra-small-body-text");
    lockText.addClassName("color-secondary");
    securityNote.add(lockIcon, lockText);
    content.add(securityNote);

    dialog.add(content);

    // Build footer buttons — button is created first, then the listener is
    // added in a separate step so the lambda can safely reference it.
    Button connectBtn = new Button("Validate & Connect");
    connectBtn.addClickListener(e -> {
          String tokenValue = tokenField.getValue();
          if (tokenValue == null || tokenValue.isBlank()) {
            // Show error in status area
            statusArea.removeAll();
            statusArea.getStyle().set("display", "block");
            statusArea.getStyle().set("background-color",
                "var(--lumo-error-color-10pct)");
            statusArea.getStyle().set("border",
                "1px solid var(--lumo-error-color)");
            var errRow = new Div();
            errRow.addClassNames("flex-horizontal", "gap-02", "items-center");
            var errIcon = VaadinIcon.WARNING.create();
            errIcon.getStyle().set("color", "var(--lumo-error-color)");
            errIcon.getStyle().set("font-size",
                "var(--lumo-icon-size-s)");
            var errText = new Span(
                "Token cannot be empty. Please paste a valid Personal "
                    + "Access Token.");
            errText.addClassName("normal-body-text");
            errText.getStyle().set("color",
                "var(--lumo-error-color)");
            errRow.add(errIcon, errText);
            statusArea.add(errRow);
            return;
          }

          // Simulate token validation (in production this would call
          // the InvenioRDM REST API)
          // For the prototype, simulate a success response after a
          // brief delay.
          var ui = UI.getCurrent();
          connectBtn.setEnabled(false);
          connectBtn.setText("Validating\u2026");

          statusArea.removeAll();
          statusArea.getStyle().set("display", "block");
          statusArea.getStyle().set("background-color",
              "var(--lumo-primary-color-10pct)");
          statusArea.getStyle().set("border",
              "1px solid var(--lumo-primary-color-30pct)");
          var validatingRow = new Div();
          validatingRow.addClassNames("flex-horizontal", "gap-02",
              "items-center");
          var spinnerIcon = VaadinIcon.SPINNER.create();
          var validatingText = new Span(
              "Validating token against " + instance.baseUrl() + "\u2026");
          validatingText.addClassName("extra-small-body-text");
          validatingRow.add(spinnerIcon, validatingText);
          statusArea.add(validatingRow);

          new Thread(() -> {
            try {
              Thread.sleep(1200);  // simulate network call
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
            ui.access(() -> {
              // Mask the token for safe display
              String masked = maskToken(tokenValue);
              configuredCredentials.put(instance.id(),
                  new RepositoryCredential(instance.id(), masked,
                      LocalDate.now()));
              dialog.close();
              refreshCredentialsContent();
              showSuccessNotification(
                  "Token connected successfully for "
                      + instance.shortName()
                      + ". You can now access restricted datasets from "
                      + "this repository.");
            });
          }).start();
        });
    connectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    var cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(cancelBtn, connectBtn);
    dialog.open();

    // Focus the token field for convenience
    tokenField.focus();
  }

  /**
   * Returns a masked representation of a token, showing only the first 4
   * and last 4 characters, e.g. "abcd••••••••wxyz".
   */
  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "****";
    }
    return token.substring(0, 4) + "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
        + token.substring(token.length() - 4);
  }

  // ── Remove Token Dialog (Story 15) ─────────────────────────────────────

  /**
   * Opens a confirmation dialog before removing the stored credentials for
   * the given InvenioRDM instance. After confirmation, the credential is
   * deleted from {@link #configuredCredentials} and the UI is refreshed.
   */
  private void openRemoveTokenDialog(InvenioRDMInstance instance) {
    var dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setCloseOnOutsideClick(false);
    dialog.setCloseOnEsc(false);
    dialog.setWidth("480px");

    var body = new Div();
    body.addClassNames("flex-vertical", "gap-03");
    body.getStyle().set("padding",
        "var(--lumo-space-s) var(--lumo-space-m)");

    // Header with warning icon
    var headerRow = new Div();
    headerRow.addClassNames("flex-horizontal", "gap-03", "items-center");
    var warnIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
    warnIcon.addClassName("icon-color-warning");
    var titleSpan = new Span("Remove Repository Connection");
    titleSpan.addClassName("heading-3");
    headerRow.add(warnIcon, titleSpan);
    body.add(headerRow);

    // Confirmation text
    var confirmText = new Div();
    confirmText.addClassName("normal-body-text");
    confirmText.getElement().setProperty("innerHTML",
        "You are about to remove the Personal Access Token for "
            + "<b>" + instance.displayName() + "</b>.");
    body.add(confirmText);

    // Impact description
    var impactBox = new Div();
    impactBox.addClassNames("flex-vertical", "gap-02");
    impactBox.getStyle().set("padding",
        "var(--lumo-space-s) var(--lumo-space-m)");
    impactBox.getStyle().set("background-color",
        "var(--lumo-contrast-5pct)");
    impactBox.getStyle().set("border-radius",
        "var(--lumo-border-radius-m)");

    var impactTitle = new Span("What happens:");
    impactTitle.addClassName("normal-body-text");
    impactTitle.getStyle().set("font-weight", "600");
    impactBox.add(impactTitle);

    var impactList = new Div();
    impactList.getElement().setProperty("innerHTML",
        "<ul style='margin:0;padding-left:1.2em;'>"
            + "<li style='margin-bottom:var(--lumo-space-xxs);'>"
            + "The stored token is <b>permanently deleted</b> from the system.</li>"
            + "<li style='margin-bottom:var(--lumo-space-xxs);'>"
            + "You will <b>no longer be able to add access-restricted datasets</b> "
            + "from " + instance.shortName() + " to your projects.</li>"
            + "<li>"
            + "Already-connected restricted datasets remain in the project but "
            + "can no longer be synced.</li>"
            + "</ul>");
    impactList.addClassName("extra-small-body-text");
    impactBox.add(impactList);

    body.add(impactBox);

    var reversalNote = new Span(
        "You can reconfigure this repository at any time by adding a new token.");
    reversalNote.addClassName("extra-small-body-text");
    reversalNote.addClassName("color-secondary");
    reversalNote.getStyle().set("display", "block");
    body.add(reversalNote);

    dialog.add(body);

    // Footer buttons
    var removeBtn = new Button("Remove Token", VaadinIcon.TRASH.create(),
        e -> {
          configuredCredentials.remove(instance.id());
          dialog.close();
          refreshCredentialsContent();
          showSuccessNotification("Repository connection removed for "
              + instance.shortName()
              + ". Access-restricted datasets from this instance "
              + "are no longer available.");
        });
    removeBtn.addThemeVariants(
        ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

    var cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

    dialog.getFooter().add(cancelBtn, removeBtn);
    dialog.open();
  }

  // ── Credentials seed data ─────────────────────────────────────────────

  /**
   * Seeds the demo with Zenodo pre-configured (token present) and FDAT
   * not configured, so the prototype shows both card states immediately.
   */
  private void seedCredentials() {
    configuredCredentials.put("zenodo",
        new RepositoryCredential("zenodo",
            "a1b2\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022c3d4",
            LocalDate.of(2025, 3, 10)));
    // FDAT is intentionally left unconfigured for demo contrast
  }

  // ── Error notification helper ──────────────────────────────────────────

  private void showErrorNotification(String message) {
    var notification = new Notification(message, 4000);
    notification.addClassName("error-toast");
    notification.setPosition(Notification.Position.BOTTOM_END);
    notification.open();
  }
}
