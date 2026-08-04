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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
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
 * <b>Associated Datasets Demo V4 — Rich Row Card Prototype</b>
 *
 * <p>Fourth iteration. Single card per row (list scannability) with every
 * property always visible — no expand toggles. High-priority properties are
 * rendered in the primary tier; medium-priority properties sit below a subtle
 * divider at a lower visual weight (smaller font, secondary color).</p>
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li><b>Listing character</b>: one card per row, top-to-bottom scan.</li>
 *   <li><b>No hidden content</b>: all properties visible without interaction.</li>
 *   <li><b>Visual hierarchy</b>, not interaction hierarchy, separates high- and
 *       medium-priority properties.</li>
 *   <li><b>Self-contained</b>: each card shows everything needed to identify,
 *       assess, and act on a connected dataset.</li>
 * </ul>
 *
 * @since 1.12.0
 */
@Profile("development")
@Route("test-view/associated-datasets-v4")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
public class AssociatedDatasetsDemoV4 extends Div {

  private static final List<String> INVENIO_INSTANCES = List.of(
      "Zenodo (zenodo.org)",
      "FDAT (fdat.uni-tuebingen.de)"
  );

  private static final String SECONDARY_COLOR = "var(--lumo-secondary-text-color)";
  private static final String DATE_FORMAT = "MMM d, yyyy";

  // ── Domain records ────────────────────────────────────────────────────

  enum AccessLevel { PUBLIC, RESTRICTED }

  record ConnectedResource(
      // High-prio
      String title,
      String pid,
      AccessLevel accessLevel,
      String version,
      String accessLink,
      LocalDate publicationDate,
      // Medium-prio
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

  record SearchableResource(
      String id, String title, String pid, AccessLevel accessLevel,
      String version, String accessLink, LocalDate publicationDate,
      String resourceProvider, String creator, String resourceType,
      String community, String description
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

  // ── Credential state ──────────────────────────────────────────────────

  record InvenioRDMInstance(
      String id, String displayName, String shortName, String baseUrl,
      String description, String tokenSetupUrl) {}

  record RepositoryCredential(String instanceId, String maskedToken, LocalDate addedOn) {}

  private static final List<InvenioRDMInstance> AVAILABLE_INSTANCES = List.of(
      new InvenioRDMInstance("zenodo", "Zenodo (zenodo.org)", "Zenodo",
          "https://zenodo.org",
          "Open-access research data repository operated by CERN.",
          "https://zenodo.org/account/settings/applications/tokens/new/"),
      new InvenioRDMInstance("fdat", "FDAT (fdat.uni-tuebingen.de)", "FDAT",
          "https://fdat.uni-tuebingen.de",
          "InvenioRDM instance operated by the University of Tübingen.",
          "https://fdat.uni-tuebingen.de/account/settings/applications/tokens/new/")
  );

  // ── Mutable state ─────────────────────────────────────────────────────

  private final List<ConnectedResource> connectedResources = new ArrayList<>();
  private final Map<String, RepositoryCredential> configuredCredentials = new HashMap<>();

  private Div cardsContainer;
  private final TextField searchField = new TextField();
  private final ComboBox<String> instanceSelector = new ComboBox<>();
  private RadioButtonGroup<String> accessFilter;
  private Button connectButton;
  private Grid<SearchableResource> searchResultsGrid;
  private Div sidebarOverlay;
  private Div sidebarPanel;
  private boolean sidebarOpen = false;
  private Span selectionCountLabel;
  private Button sidebarConnectBtn;
  private Span resourceCountSpan;
  private Div credentialsContentContainer;

  public AssociatedDatasetsDemoV4() {
    addClassNames("padding-horizontal-07", "padding-vertical-04", "flex-vertical");

    seedConnectedResources();
    seedCredentials();

    var pageTitle = new Div("Connected Resources — UI Prototype V4 (Rich Row Cards)");
    pageTitle.addClassName("heading-1");
    add(pageTitle);

    var subtitle = new Span(
        "Single card per row: listing scannability with full property visibility. "
            + "High-priority properties in the primary tier; medium-priority below "
            + "the divider at reduced visual weight — always visible, no toggles.");
    subtitle.addClassName("normal-body-text");
    subtitle.addClassName("color-secondary");
    subtitle.getStyle().set("max-width", "760px");
    add(subtitle);
    add(new Div());

    add(buildConnectedResourcesSection());
    add(buildRepositoryCredentialsSection());
    buildConnectSidebar();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CONNECTED RESOURCES — Rich Row Card List
  // ══════════════════════════════════════════════════════════════════════

  private Section buildConnectedResourcesSection() {
    var section = new Section.SectionBuilder().build();

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
        new SectionNote("Datasets connected from InvenioRDM repositories.")
    );
    header.enableControls();
    section.setHeader(header);

    var content = new SectionContent();

    // ── Filter bar ────────────────────────────────────────────────
    var filterBar = new Div();
    filterBar.addClassNames("flex-horizontal", "gap-03", "items-center");
    filterBar.getStyle().set("margin-bottom", "var(--lumo-space-m)");

    var filterLabel = new Span("Show:");
    filterLabel.addClassName("normal-body-text");
    filterLabel.getStyle().set("font-weight", "500");

    accessFilter = new RadioButtonGroup<>();
    accessFilter.setItems("All", "Public", "Restricted");
    accessFilter.setValue("All");
    accessFilter.addValueChangeListener(e -> refreshCards());

    resourceCountSpan = new Span();
    resourceCountSpan.addClassName("extra-small-body-text");
    resourceCountSpan.addClassName("color-secondary");
    resourceCountSpan.getStyle().set("margin-left", "auto");

    filterBar.add(filterLabel, accessFilter, resourceCountSpan);
    content.add(filterBar);

    // ── Card container ────────────────────────────────────────────
    cardsContainer = new Div();
    cardsContainer.addClassNames("flex-vertical");
    cardsContainer.getStyle().set("gap", "var(--lumo-space-m)");

    refreshCards();
    content.add(cardsContainer);

    section.setContent(content);
    return section;
  }

  /**
   * Builds a single rich row card.
   *
   * Layout:
   * <pre>
   * ┌──────────────────────────────────────────────────────────────────────┐
   * │ [Provider] [Access] connected by X              YYYY-MM-DD     ⋮    │  ← Header row
   * │                                                                      │
   * │ Dataset title (bold, primary)                          [Sync] [✕]    │  ← Title + actions
   * │                                                                      │
   * │ doi: 10.5281/...  ·  v1  ·  🔗 Open ↗                              │  ← PID · Version · Link
   * ├──────────────────────────────────────────────────────────────────────┤
   * │ Creator: …  ·  Type: …  ·  Community: …  ·  Experiment: …          │  ← Medium-prio tier
   * └──────────────────────────────────────────────────────────────────────┘
   * </pre>
   */
  private Component buildResourceCard(ConnectedResource resource) {
    var card = new Div();
    card.addClassNames("border", "rounded-02");
    card.getStyle().set("display", "flex");
    card.getStyle().set("flex-direction", "column");
    card.getStyle().set("padding", "0");
    card.getStyle().set("overflow", "hidden");
    card.getStyle().set("background-color", "var(--lumo-base-color)");

    // Update-available: left border accent instead of top border
    if (resource.updateAvailable()) {
      card.getStyle().set("border-left", "3px solid var(--lumo-warning-color)");
    }

    // ═══ HEADER ROW: Provider · Access · Date ═══
    var headerRow = new Div();
    headerRow.addClassNames("flex-horizontal", "items-center", "gap-03");
    headerRow.getStyle().set("padding",
        "var(--lumo-space-m) var(--lumo-space-m) var(--lumo-space-s) var(--lumo-space-m)");
    headerRow.getStyle().set("flex-wrap", "wrap");

    var providerTag = new Tag(resource.resourceProvider());
    providerTag.setTagColor(
        "Zenodo".equals(resource.resourceProvider()) ? TagColor.PRIMARY : TagColor.TEAL);
    headerRow.add(providerTag);

    var accessBadge = new Tag(
        resource.accessLevel() == AccessLevel.PUBLIC ? "Public" : "Restricted");
    accessBadge.setTagColor(
        resource.accessLevel() == AccessLevel.PUBLIC ? TagColor.SUCCESS : TagColor.WARNING);
    headerRow.add(accessBadge);

    // Update-available badge in header (primary indicator)
    if (resource.updateAvailable()) {
      var updateBadge = new Tag("Update available");
      updateBadge.setTagColor(TagColor.WARNING);
      headerRow.add(updateBadge);
    }

    // Spacer — pushes date to the right
    var headerSpacer = new Div();
    headerSpacer.getStyle().set("flex-grow", "1");
    headerRow.add(headerSpacer);

    // Published date
    var dateSpan = new Span("Published: " + resource.publicationDate().format(
        DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.ENGLISH)));
    dateSpan.addClassName("extra-small-body-text");
    dateSpan.getStyle().set("color", SECONDARY_COLOR);
    headerRow.add(dateSpan);

    card.add(headerRow);

    // ═══ TITLE BLOCK ═══
    var titleBlock = new Div();
    titleBlock.getStyle().set("padding",
        "0 var(--lumo-space-m) var(--lumo-space-m) var(--lumo-space-m)");
    titleBlock.getStyle().set("min-width", "0");

    var titleSpan = new Span(resource.title());
    titleSpan.addClassNames("normal-body-text");
    titleSpan.getStyle().set("font-weight", "600");
    titleSpan.getStyle().set("line-height", "1.4");
    titleBlock.add(titleSpan);

    card.add(titleBlock);

    // ═══ META ROW: PID · Version · Access Link ═══
    var metaRow = new Div();
    metaRow.addClassNames("flex-horizontal", "items-center", "gap-03");
    metaRow.getStyle().set("padding",
        "0 var(--lumo-space-m) var(--lumo-space-m) var(--lumo-space-m)");
    metaRow.getStyle().set("flex-wrap", "wrap");

    String pidHref = resource.pid().startsWith("http")
        ? resource.pid() : "https://doi.org/" + resource.pid();
    var pidLink = new Anchor(pidHref, resource.pid());
    pidLink.setTarget(AnchorTarget.BLANK);
    pidLink.addClassName("extra-small-body-text");
    metaRow.add(pidLink);

    addMetaSeparator(metaRow);

    String vNorm = resource.version() != null
        ? resource.version().replaceFirst("^v", "") : "—";
    var versionSpan = new Span("v" + vNorm);
    versionSpan.addClassName("extra-small-body-text");
    versionSpan.getStyle().set("color", SECONDARY_COLOR);
    metaRow.add(versionSpan);

    if (resource.accessLink() != null && !resource.accessLink().isBlank()) {
      addMetaSeparator(metaRow);
      var accessAnchor = new Anchor(resource.accessLink(),
          "Open on " + resource.resourceProvider() + " ↗");
      accessAnchor.setTarget(AnchorTarget.BLANK);
      accessAnchor.addClassName("extra-small-body-text");
      metaRow.add(accessAnchor);
    }

    card.add(metaRow);

    // ═══ MEDIUM-PRIO TIER ═══
    var detailRow = new Div();
    detailRow.addClassNames("flex-horizontal", "items-start");
    detailRow.getStyle().set("padding",
        "var(--lumo-space-m) var(--lumo-space-m)");
    detailRow.getStyle().set("flex-wrap", "wrap");
    detailRow.getStyle().set("gap", "var(--lumo-space-m) var(--lumo-space-xl)");
    detailRow.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
    detailRow.getStyle().set("border-top", "2px solid var(--lumo-contrast-10pct)");

    addDetailCell(detailRow, "Connected by", resource.connectedBy());
    addDetailCell(detailRow, "Type",
        resource.resourceType() != null ? resource.resourceType() : "—");
    addDetailCell(detailRow, "Community",
        resource.community() != null ? resource.community() : "—");
    addDetailCell(detailRow, "Experiment",
        resource.linkedExperiment() != null ? resource.linkedExperiment() : "—");
    addDetailCell(detailRow, "Connected on",
        resource.connectedOn() != null
            ? resource.connectedOn().format(
                DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.ENGLISH))
            : "—");

    // Access nuance for restricted datasets
    if (resource.accessLevel() == AccessLevel.RESTRICTED) {
      addDetailCell(detailRow, "Access detail", "Record: public · Files: restricted");
    }

    card.add(detailRow);

    // ═══ FOOTER: Actions ═══
    var footer = new Div();
    footer.addClassNames("flex-horizontal", "items-center");
    footer.getStyle().set("padding",
        "var(--lumo-space-m) var(--lumo-space-m) var(--lumo-space-s) var(--lumo-space-m)");
    footer.getStyle().set("justify-content", "flex-end");
    footer.getStyle().set("gap", "var(--lumo-space-s)");
    footer.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
    footer.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");

    var removeBtn = new Button("Remove", VaadinIcon.TRASH.create(),
        e -> confirmRemoveResource(resource));
    removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    removeBtn.setTooltipText("Remove connection");

    var syncBtn = new Button("Sync", VaadinIcon.REFRESH.create(),
        e -> syncSingleResource(resource));
    syncBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    syncBtn.setTooltipText("Check for updates");
    if (resource.updateAvailable()) {
      syncBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      syncBtn.setTooltipText("Update available — click to sync");
    }

    footer.add(removeBtn, syncBtn);
    card.add(footer);


    return card;
  }

  private void addMetaSeparator(Div row) {
    var sep = new Span("·");
    sep.addClassName("extra-small-body-text");
    sep.getStyle().set("color", "var(--lumo-contrast-30pct)");
    row.add(sep);
  }

  private void addDetailCell(Div container, String label, String value) {
    var cell = new Div();
    cell.addClassNames("flex-horizontal", "gap-02", "items-baseline");

    var labelSpan = new Span(label + ":");
    labelSpan.addClassName("extra-small-body-text");
    labelSpan.getStyle().set("font-weight", "600");
    labelSpan.getStyle().set("color", SECONDARY_COLOR);

    var valueSpan = new Span(value != null ? value : "—");
    valueSpan.addClassNames("extra-small-body-text");

    cell.add(labelSpan, valueSpan);
    container.add(cell);
  }

  // ── Card refresh & filtering ──────────────────────────────────────

  private void refreshCards() {
    cardsContainer.removeAll();
    String filter = accessFilter.getValue();

    List<ConnectedResource> filtered;
    if ("Public".equals(filter)) {
      filtered = connectedResources.stream()
          .filter(r -> r.accessLevel() == AccessLevel.PUBLIC).toList();
    } else if ("Restricted".equals(filter)) {
      filtered = connectedResources.stream()
          .filter(r -> r.accessLevel() == AccessLevel.RESTRICTED).toList();
    } else {
      filtered = new ArrayList<>(connectedResources);
    }

    if (filtered.isEmpty()) {
      cardsContainer.add(buildEmptyState());
    } else {
      for (ConnectedResource resource : filtered) {
        cardsContainer.add(buildResourceCard(resource));
      }
    }
    updateResourceCount();
  }

  private Div buildEmptyState() {
    var wrapper = new Div();
    wrapper.addClassNames("flex-vertical", "items-center");
    wrapper.getStyle().set("padding", "var(--lumo-space-xl) 0");
    wrapper.getStyle().set("text-align", "center");

    var icon = VaadinIcon.DATABASE.create();
    icon.getStyle().set("font-size", "64px");
    icon.getStyle().set("color", "var(--lumo-contrast-30pct)");
    icon.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    wrapper.add(icon);

    var heading = new Span("No datasets connected");
    heading.addClassName("heading-3");
    wrapper.add(heading);

    var explanation = new Span(
        "Connect open datasets from InvenioRDM repositories (Zenodo, FDAT) "
            + "to keep an overview of all associated data in one place.");
    explanation.addClassName("normal-body-text");
    explanation.addClassName("color-secondary");
    explanation.getStyle().set("max-width", "460px");
    explanation.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    wrapper.add(explanation);

    var cta = new Button("Connect Datasets", VaadinIcon.PLUS_CIRCLE.create());
    cta.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    cta.addClickListener(e -> openConnectSidebar());
    wrapper.add(cta);
    return wrapper;
  }

  private void updateResourceCount() {
    int total = connectedResources.size();
    long pub = connectedResources.stream()
        .filter(r -> r.accessLevel() == AccessLevel.PUBLIC).count();
    if (resourceCountSpan != null) {
      resourceCountSpan.setText("%d resource(s) — %d public, %d restricted"
          .formatted(total, pub, total - pub));
    }
  }

  // ══════════════════════════════════════════════════════════════════════
  //  SIDEBAR (connect flow, reused from V3)
  // ══════════════════════════════════════════════════════════════════════

  private void buildConnectSidebar() {
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

    var body = new Div();
    body.getStyle().set("height", "100%");
    body.getStyle().set("box-sizing", "border-box");
    body.getStyle().set("display", "flex");
    body.getStyle().set("flex-direction", "column");

    // Header
    var sh = new Div();
    sh.addClassNames("flex-horizontal", "items-center");
    sh.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    sh.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
    sh.getStyle().set("flex-shrink", "0");
    sh.getStyle().set("gap", "var(--lumo-space-s)");
    var st = new Span("Connect Datasets");
    st.addClassName("heading-3");
    st.getStyle().set("flex-grow", "1");
    var cb = new Button(VaadinIcon.CLOSE_SMALL.create());
    cb.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    cb.addClickListener(e -> closeConnectSidebar());
    sh.add(st, cb);
    body.add(sh);

    // Content
    var ca = new Div();
    ca.getStyle().set("flex-grow", "1");
    ca.getStyle().set("overflow-y", "auto");
    ca.getStyle().set("padding", "var(--lumo-space-l)");

    var info = new InfoBox().setInfoText(
        "Search InvenioRDM repositories and connect datasets to this project.");
    info.setClosable(true);
    info.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    ca.add(info);

    // Search form
    instanceSelector.setItems(INVENIO_INSTANCES);
    instanceSelector.setPlaceholder("Select repository…");
    instanceSelector.setValue(INVENIO_INSTANCES.get(0));
    instanceSelector.setWidth("180px");
    instanceSelector.setLabel("Repository");
    instanceSelector.addClassName("connect-dataset-sidebar-overlay");
    instanceSelector.addValueChangeListener(e -> performSearch());

    searchField.setPlaceholder("Search by title, DOI, or creator…");
    searchField.setClearButtonVisible(true);
    searchField.getStyle().set("flex-grow", "1");

    var searchRow = new Div();
    searchRow.getStyle().set("display", "flex");
    searchRow.getStyle().set("gap", "var(--lumo-space-xs)");
    searchRow.getStyle().set("align-items", "flex-end");
    searchRow.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    searchRow.add(instanceSelector, searchField);

    var btnRow = new Div();
    btnRow.getStyle().set("display", "flex");
    btnRow.getStyle().set("gap", "var(--lumo-space-xs)");

    var searchBtn = new Button("Search", VaadinIcon.SEARCH.create());
    searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    searchBtn.addClickListener(e -> performSearch());
    searchField.addKeyDownListener(Key.ENTER, e -> performSearch());

    var clearBtn = new Button("Clear");
    clearBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    clearBtn.addClickListener(e -> { searchField.clear(); performSearch(); });

    btnRow.add(searchBtn, clearBtn);
    ca.add(searchRow, btnRow);

    // Results
    searchResultsGrid = new Grid<>();
    searchResultsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_ROW_BORDERS);
    searchResultsGrid.setSelectionMode(SelectionMode.MULTI);
    searchResultsGrid.setWidthFull();
    searchResultsGrid.setAllRowsVisible(true);
    searchResultsGrid.addComponentColumn(this::buildSearchCard).setFlexGrow(1).setKey("card");
    searchResultsGrid.addSelectionListener(ev -> {
      int n = ev.getAllSelectedItems().size();
      selectionCountLabel.setText(n == 0 ? "" : n + " selected");
      sidebarConnectBtn.setEnabled(n > 0);
    });
    ca.add(searchResultsGrid);
    body.add(ca);

    // Footer
    var footer = new Div();
    footer.addClassNames("flex-horizontal", "items-center");
    footer.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    footer.getStyle().set("border-top", "1px solid var(--lumo-contrast-10pct)");
    footer.getStyle().set("flex-shrink", "0");
    footer.getStyle().set("gap", "var(--lumo-space-s)");

    selectionCountLabel = new Span("");
    selectionCountLabel.addClassName("normal-body-text");
    selectionCountLabel.addClassName("color-secondary");
    selectionCountLabel.getStyle().set("flex-grow", "1");

    sidebarConnectBtn = new Button("Connect Selected", VaadinIcon.PLUS_CIRCLE.create());
    sidebarConnectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sidebarConnectBtn.setEnabled(false);
    sidebarConnectBtn.addClickListener(e -> connectSelected());

    footer.add(selectionCountLabel, sidebarConnectBtn);
    body.add(footer);

    sidebarPanel.add(body);
    add(sidebarOverlay, sidebarPanel);
  }

  private Div buildSearchCard(SearchableResource r) {
    var card = new Div();
    card.addClassName("border");
    card.addClassName("rounded-02");
    card.getStyle().set("padding", "var(--lumo-space-m)");
    card.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    card.getStyle().set("cursor", "pointer");

    var top = new Div();
    top.addClassNames("flex-horizontal", "items-center", "gap-02");
    top.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
    Tag ab = r.accessLevel() == AccessLevel.PUBLIC
        ? new Tag("Public") : new Tag("Restricted");
    ab.setTagColor(r.accessLevel() == AccessLevel.PUBLIC ? TagColor.SUCCESS : TagColor.WARNING);
    top.add(ab);
    Tag pt = new Tag(r.resourceProvider());
    pt.setTagColor("Zenodo".equals(r.resourceProvider()) ? TagColor.PRIMARY : TagColor.TEAL);
    top.add(pt);
    var ds = new Span(r.publicationDate().format(
        DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.ENGLISH)));
    ds.addClassName("extra-small-body-text");
    ds.addClassName("color-secondary");
    ds.getStyle().set("margin-left", "auto");
    top.add(ds);
    card.add(top);

    var title = new Span(r.title());
    title.addClassName("normal-body-text");
    title.getStyle().set("font-weight", "600");
    title.getStyle().set("display", "block");
    title.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
    card.add(title);

    var meta = new Div();
    meta.addClassNames("flex-horizontal", "gap-04", "items-center");
    var creatorLbl = new Span("by " + r.creator());
    creatorLbl.addClassName("extra-small-body-text");
    creatorLbl.addClassName("color-secondary");
    meta.add(creatorLbl);
    var pidAnchor = new Anchor(
        r.pid().startsWith("http") ? r.pid() : "https://doi.org/" + r.pid(),
        r.pid());
    pidAnchor.setTarget(AnchorTarget.BLANK);
    pidAnchor.addClassName("extra-small-body-text");
    meta.add(pidAnchor);
    card.add(meta);
    return card;
  }

  private void openConnectSidebar() {
    sidebarOpen = true;
    sidebarOverlay.getStyle().set("display", "block");
    sidebarPanel.getStyle().set("display", "block");
    performSearch();
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
  //  SEARCH
  // ══════════════════════════════════════════════════════════════════════

  private void performSearch() {
    String repo = instanceSelector.getValue();
    String term = searchField.getValue();
    var repoFiltered = MOCK_SEARCH_RESULTS.stream()
        .filter(r -> matchesRepo(r, repo))
        .toList();
    if (term == null || term.isBlank()) {
      searchResultsGrid.setItems(new ArrayList<>(repoFiltered));
      return;
    }
    String lo = term.toLowerCase();
    searchResultsGrid.setItems(repoFiltered.stream()
        .filter(r -> r.title().toLowerCase().contains(lo)
            || r.pid().toLowerCase().contains(lo)
            || r.creator().toLowerCase().contains(lo)
            || r.description().toLowerCase().contains(lo))
        .toList());
  }

  private boolean matchesRepo(SearchableResource r, String repo) {
    if (repo == null || repo.isBlank()) return true;
    int idx = repo.indexOf(' ');
    String provider = idx > 0 ? repo.substring(0, idx) : repo;
    return r.resourceProvider().equals(provider);
  }

  // ══════════════════════════════════════════════════════════════════════
  //  ACTIONS
  // ══════════════════════════════════════════════════════════════════════

  private void connectSelected() {
    var sel = searchResultsGrid.getSelectedItems();
    if (sel.isEmpty()) return;
    for (SearchableResource sr : sel) {
      connectedResources.add(new ConnectedResource(
          sr.title(), sr.pid(), sr.accessLevel(), sr.version(),
          sr.accessLink(), sr.publicationDate(),
          "Current User (demo)", sr.resourceProvider(), sr.creator(),
          sr.resourceType(), sr.community(), null,
          sr.id(), LocalDate.now(), false));
    }
    refreshCards();
    searchResultsGrid.deselectAll();
    closeConnectSidebar();
    notify(sel.size() + " dataset(s) connected to this project.", "success-toast");
  }

  private void confirmRemoveResource(ConnectedResource r) {
    var d = new com.vaadin.flow.component.dialog.Dialog();
    d.setCloseOnOutsideClick(false);
    d.setCloseOnEsc(false);
    d.setWidth("480px");
    var body = new Div();
    body.addClassNames("flex-vertical", "gap-03", "padding-horizontal-05", "padding-vertical-04");
    var hr = new Div();
    hr.addClassNames("flex-horizontal", "gap-03");
    var removeTitle = new Span("Remove Dataset Connection");
    removeTitle.addClassName("heading-3");
    hr.add(VaadinIcon.EXCLAMATION_CIRCLE.create(), removeTitle);
    body.add(hr);
    body.add(new Span("Are you sure you want to remove the connection to:"));
    var lbl = new Span(r.title());
    lbl.getStyle().set("font-weight", "600");
    body.add(lbl);
    body.add(new Span("This only removes the link from this project, "
        + "not the dataset on " + r.resourceProvider() + "."));
    var foot = new Div();
    foot.addClassNames("flex-horizontal", "gap-03", "padding-horizontal-05", "padding-vertical-03");
    foot.getStyle().set("justify-content", "flex-end");
    var cancel = new Button("Cancel", e -> d.close());
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    var remove = new Button("Remove", VaadinIcon.TRASH.create(), e -> {
      connectedResources.removeIf(x -> x.id().equals(r.id()));
      refreshCards();
      d.close();
      notify("Connection removed: " + r.title(), "success-toast");
    });
    remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
    foot.add(cancel, remove);
    d.add(body);
    d.getFooter().add(foot);
    d.open();
  }

  private void syncSingleResource(ConnectedResource r) {
    notify("Syncing '" + r.title() + "' with " + r.resourceProvider() + "…", "info-toast");
    var ui = UI.getCurrent();
    new Thread(() -> {
      try { Thread.sleep(1500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      ui.access(() -> notify("Sync complete: " + r.title(), "info-toast"));
    }).start();
  }

  private void syncAllResources() {
    if (connectedResources.isEmpty()) {
      notify("No resources connected to sync.", "info-toast");
      return;
    }
    notify("Syncing " + connectedResources.size() + " resources…", "info-toast");
    var ui = UI.getCurrent();
    new Thread(() -> {
      try { Thread.sleep(2000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      ui.access(() -> notify("All " + connectedResources.size() + " resources are up to date.", "success-toast"));
    }).start();
  }

  private void notify(String msg, String cssClass) {
    var n = new Notification(msg, 3000);
    n.addClassName(cssClass);
    n.setPosition(Notification.Position.BOTTOM_END);
    n.open();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CREDENTIALS SECTION
  // ══════════════════════════════════════════════════════════════════════

  private Section buildRepositoryCredentialsSection() {
    var section = new Section.SectionBuilder().build();
    section.setHeader(new SectionHeader(
        new SectionTitle("Repository Access — Account Settings"),
        new ActionBar(),
        new SectionNote("Configure Personal Access Tokens for InvenioRDM repositories. "
            + "Restricts access to datasets that are not publicly visible. Public datasets "
            + "work without credentials.")
    ));
    var content = new SectionContent();
    credentialsContentContainer = new Div();
    credentialsContentContainer.addClassNames("flex-vertical", "gap-03");
    refreshCredentials();
    content.add(credentialsContentContainer);
    section.setContent(content);
    return section;
  }

  private void refreshCredentials() {
    credentialsContentContainer.removeAll();
    var callout = new Div();
    callout.addClassNames("border", "rounded-02");
    callout.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    callout.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
    callout.getStyle().set("border-color", "var(--lumo-primary-color-30pct)");
    var hdr = new Div();
    hdr.addClassNames("flex-horizontal", "gap-02", "items-center");
    var icon = VaadinIcon.LIGHTBULB.create();
    icon.getStyle().set("color", "var(--lumo-primary-color)");
    var cfgTitle = new Span("Why configure repository access?");
    cfgTitle.getStyle().set("font-weight", "600");
    hdr.add(icon, cfgTitle);
    callout.add(hdr);
    var body = new Div();
    body.getElement().setProperty("innerHTML",
        "<ul style='margin:var(--lumo-space-xs) 0 0;padding-left:1.2em;'>"
            + "<li>Search and connect <b>access-restricted datasets</b></li>"
            + "<li>Overview of <b>public and restricted</b> data in one place</li>"
            + "<li>Token stored <b>encrypted in the vault</b></li>"
            + "</ul>");
    callout.add(body);
    credentialsContentContainer.add(callout);
    for (var inst : AVAILABLE_INSTANCES) credentialsContentContainer.add(instCard(inst));
  }

  private Div instCard(InvenioRDMInstance inst) {
    var card = new Div();
    card.addClassNames("border", "rounded-02");
    card.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    boolean cfg = configuredCredentials.containsKey(inst.id());
    var top = new Div();
    top.addClassNames("flex-horizontal", "items-center", "gap-03");
    top.getStyle().set("margin-bottom", "var(--lumo-space-xs)");
    var instName = new Span(inst.displayName());
    instName.getStyle().set("font-weight", "600");
    instName.getStyle().set("flex-grow", "1");
    top.add(VaadinIcon.CLOUD.create(), instName);
    if (cfg) {
      var connTag = new Tag("\u2713 Connected");
      connTag.setTagColor(TagColor.SUCCESS);
      top.add(connTag);
    } else {
      var ncTag = new Tag("Not configured");
      ncTag.setTagColor(TagColor.CONTRAST);
      top.add(ncTag);
    }
    card.add(top);
    var descLbl = new Span(inst.description());
    descLbl.addClassName("extra-small-body-text");
    descLbl.addClassName("color-secondary");
    descLbl.getStyle().set("display", "block");
    descLbl.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    card.add(descLbl);
    var row = new Div();
    row.addClassNames("flex-horizontal", "items-center");
    row.getStyle().set("gap", "var(--lumo-space-s)");
    if (cfg) {
      RepositoryCredential c = configuredCredentials.get(inst.id());
      var info = new Span("Token: " + c.maskedToken() + "  |  Added: "
          + c.addedOn().format(DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.ENGLISH)));
      info.addClassName("extra-small-body-text");
      info.addClassName("color-secondary");
      info.getStyle().set("flex-grow", "1");
      var rm = new Button("Remove", VaadinIcon.TRASH.create(), e -> openRemoveToken(inst));
      rm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
      row.add(info, rm);
    } else {
      var hint = new Span("No personal access token configured.");
      hint.addClassName("extra-small-body-text");
      hint.addClassName("color-secondary");
      hint.getStyle().set("flex-grow", "1");
      var add = new Button("Configure", VaadinIcon.PLUS_CIRCLE.create(), e -> openAddToken(inst));
      add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      row.add(hint, add);
    }
    card.add(row);
    return card;
  }

  private void openAddToken(InvenioRDMInstance inst) {
    var d = new com.vaadin.flow.component.dialog.Dialog();
    d.setHeaderTitle("Connect " + inst.displayName());
    d.setWidth("520px");
    d.setCloseOnOutsideClick(false);
    var body = new Div();
    body.addClassNames("flex-vertical", "gap-03");
    body.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
    var intro = new Div();
    intro.addClassName("normal-body-text");
    intro.getElement().setProperty("innerHTML",
        "Provide a Personal Access Token from <b>" + inst.shortName() + "</b> "
            + "to connect access-restricted datasets.");
    body.add(intro);
    var tf = new PasswordField();
    tf.setLabel("Personal Access Token");
    tf.setPlaceholder("Paste your token…");
    tf.setWidthFull();
    tf.setRequiredIndicatorVisible(true);
    body.add(tf);
    var link = new Anchor(inst.tokenSetupUrl(), "Open " + inst.shortName() + " token settings ↗");
    link.setTarget(AnchorTarget.BLANK);
    link.addClassName("extra-small-body-text");
    body.add(link);
    var hint = new Span("Prototype: any non-empty token is accepted.");
    hint.addClassName("extra-small-body-text");
    hint.addClassName("color-secondary");
    hint.getStyle().set("font-style", "italic");
    body.add(hint);
    d.add(body);
    var ok = new Button("Validate & Connect");
    ok.addClickListener(click -> {
      if (tf.getValue() == null || tf.getValue().isBlank()) {
        tf.setInvalid(true);
        tf.setErrorMessage("Token cannot be empty.");
        return;
      }
      var ui = UI.getCurrent();
      ok.setEnabled(false);
      ok.setText("Validating…");
      new Thread(() -> {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
        ui.access(() -> {
          String m = tf.getValue().length() <= 8 ? "****"
              : tf.getValue().substring(0, 4) + "••••••••" + tf.getValue().substring(tf.getValue().length() - 4);
          configuredCredentials.put(inst.id(), new RepositoryCredential(inst.id(), m, LocalDate.now()));
          d.close();
          refreshCredentials();
          notify("Connected: " + inst.shortName(), "success-toast");
        });
      }).start();
    });
    ok.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    var cancel = new Button("Cancel", e -> d.close());
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    d.getFooter().add(cancel, ok);
    d.open();
    tf.focus();
  }

  private void openRemoveToken(InvenioRDMInstance inst) {
    var d = new com.vaadin.flow.component.dialog.Dialog();
    d.setWidth("480px");
    d.setCloseOnOutsideClick(false);
    d.setCloseOnEsc(false);
    var body = new Div();
    body.addClassNames("flex-vertical", "gap-03");
    body.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
    var hr = new Div();
    hr.addClassNames("flex-horizontal", "gap-03", "items-center");
    var icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
    icon.addClassName("icon-color-warning");
    var rmTitle = new Span("Remove Repository Connection");
    rmTitle.addClassName("heading-3");
    hr.add(icon, rmTitle);
    body.add(hr);
    body.add(new Span("Remove token for " + inst.displayName() + "?"));
    var reconfSpan = new Span("You can reconfigure at any time.");
    reconfSpan.addClassName("extra-small-body-text");
    reconfSpan.addClassName("color-secondary");
    body.add(reconfSpan);
    d.add(body);
    var rm = new Button("Remove Token", VaadinIcon.TRASH.create(), e -> {
      configuredCredentials.remove(inst.id());
      d.close();
      refreshCredentials();
      notify("Token removed for " + inst.shortName(), "success-toast");
    });
    rm.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
    var cancel = new Button("Cancel", e -> d.close());
    cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    d.getFooter().add(cancel, rm);
    d.open();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  SEED DATA
  // ══════════════════════════════════════════════════════════════════════

  private void seedConnectedResources() {
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

  private void seedCredentials() {
    configuredCredentials.put("zenodo",
        new RepositoryCredential("zenodo",
            "a1b2••••••••c3d4", LocalDate.of(2025, 3, 10)));
  }
}
