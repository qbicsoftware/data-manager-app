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
import com.vaadin.flow.component.html.Hr;
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
 * <b>Associated Datasets Demo V3 — Card Layout Prototype</b>
 *
 * <p>Third iteration of the UI prototype for "Connect datasets with research projects".
 * This version replaces the columnar grid (V2) with a <b>card-based layout</b>
 * for connected datasets, following feedback that the tabular grid makes it hard
 * to visually distinguish individual datasets when many are connected.</p>
 *
 * <h3>Design rationale</h3>
 * <ul>
 *   <li><b>Cards</b> provide clear visual boundaries per dataset, making it
 *       immediately obvious what belongs together.</li>
 *   <li><b>Responsive 2-column grid</b> (collapses to 1 column on narrow screens)
 *       balances information density and readability.</li>
 *   <li><b>Property hierarchy</b> from the stakeholder specification is respected:
 *       <ul>
 *         <li><i>High-prio</i> (Title, PID, Access Status, Version, Access Link,
 *             Publication Date) are always visible in the card body.</li>
 *         <li><i>Medium-prio</i> (Connected By, Resource Provider, Creator,
 *             Resource Type, Community, Linked Experiment) live in a
 *             collapsible details section within each card.</li>
 *       </ul>
 *   </li>
 *   <li><b>Inline actions</b> (Sync, Remove) at the bottom of each card —
 *       no need to hover or expand.</li>
 * </ul>
 *
 * <p>The connect sidebar and credentials section are reused from V2.</p>
 *
 * <p>Only available with the {@code development} profile.</p>
 *
 * @since 1.12.0
 */
@Profile("development")
@Route("test-view/associated-datasets-v3")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
public class AssociatedDatasetsDemoV3 extends Div {

  private static final List<String> INVENIO_INSTANCES = List.of(
      "Zenodo (zenodo.org)",
      "FDAT (fdat.uni-tuebingen.de)"
  );

  // ── Domain records ────────────────────────────────────────────────────

  enum AccessLevel { PUBLIC, RESTRICTED }

  /** A connected dataset resource displayed as a card. */
  record ConnectedResource(
      // High-prio (stakeholder doc)
      String title,
      String pid,
      AccessLevel accessLevel,
      String version,
      String accessLink,
      LocalDate publicationDate,
      // Medium-prio (stakeholder doc)
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

  record InvenioRDMInstance(
      String id,
      String displayName,
      String shortName,
      String baseUrl,
      String description,
      String tokenSetupUrl
  ) {}

  record RepositoryCredential(
      String instanceId,
      String maskedToken,
      LocalDate addedOn
  ) {}

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
  private final Map<String, RepositoryCredential> configuredCredentials = new HashMap<>();

  private Div cardsContainer;
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
  private Span resourceCountSpan;

  public AssociatedDatasetsDemoV3() {
    addClassNames("padding-horizontal-07", "padding-vertical-04");
    addClassName("flex-vertical");

    seedConnectedResources();
    seedCredentials();

    // ── Page heading ────────────────────────────────────────────────
    var title = new Div("Connected Resources — UI Prototype V3 (Card Layout)");
    title.addClassName("heading-1");
    add(title);

    var subtitle = new Div(
        "Card layout: each dataset is rendered as an individual card with "
            + "high-priority properties always visible and medium-priority "
            + "properties in a collapsible details section. Compare with "
            + "V2 (columnar grid).");
    subtitle.addClassName("normal-body-text");
    subtitle.addClassName("color-secondary");
    subtitle.getStyle().set("max-width", "720px");
    add(subtitle);
    add(new Div()); // spacer

    // ── Main content: Connected Resources section ──────────────────
    add(buildConnectedResourcesSection());

    // ── Repository credentials section (stories 14 & 15) ─────────
    add(buildRepositoryCredentialsSection());

    // ── Sidebar overlay + panel (initially hidden) ────────────────
    buildConnectSidebar();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CONNECTED RESOURCES — Card Layout  (High-Prio properties)
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
            + "Each dataset is shown as a card with key properties. "
            + "Expand a card for additional metadata.")
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
    accessFilter.addValueChangeListener(e -> refreshResourceCards());

    resourceCountSpan = new Span();
    resourceCountSpan.addClassName("extra-small-body-text");
    resourceCountSpan.addClassName("color-secondary");
    resourceCountSpan.getStyle().set("margin-left", "auto");
    updateResourceCount();

    filterBar.add(filterLabel, accessFilter, resourceCountSpan);
    content.add(filterBar);

    // ── Cards container (CSS Grid, responsive) ────────────────────
    cardsContainer = new Div();
    cardsContainer.getStyle().set("display", "grid");
    cardsContainer.getStyle().set("grid-template-columns",
        "repeat(auto-fill, minmax(420px, 1fr))");
    cardsContainer.getStyle().set("gap", "var(--lumo-space-m)");

    refreshResourceCards();
    content.add(cardsContainer);
    section.setContent(content);
    return section;
  }

  /**
   * Builds a single card for a connected dataset.
   *
   * <p>Layout follows the stakeholder property hierarchy:</p>
   * <ol>
   *   <li><b>Header row</b>: Provider tag + Access badge + Publication date</li>
   *   <li><b>Title</b>: Bold, prominent — the primary identity of the dataset</li>
   *   <li><b>Meta row</b>: PID (linked DOI) · Version · Access link</li>
   *   <li><b>Details section</b> (collapsible): Medium-prio properties</li>
   *   <li><b>Footer row</b>: Connected date + Sync/Remove actions</li>
   * </ol>
   */
  private Component buildResourceCard(ConnectedResource resource) {
    var card = new Div();
    card.addClassNames("border", "rounded-02");
    card.getStyle().set("display", "flex");
    card.getStyle().set("flex-direction", "column");
    card.getStyle().set("padding", "0");
    card.getStyle().set("overflow", "hidden");
    card.getStyle().set("background-color", "var(--lumo-base-color)");

    // Update-available indicator: subtle top border accent
    if (resource.updateAvailable()) {
      card.getStyle().set("border-top", "3px solid var(--lumo-warning-color)");
    }

    // ── Card header: Provider + Access + Date ────────────────────
    var cardHeader = new Div();
    cardHeader.addClassNames("flex-horizontal", "items-center", "gap-02");
    cardHeader.getStyle().set("padding",
        "var(--lumo-space-m) var(--lumo-space-m) var(--lumo-space-s) var(--lumo-space-m)");
    cardHeader.getStyle().set("flex-wrap", "wrap");

    // Provider tag
    var providerTag = new Tag(resource.resourceProvider());
    providerTag.setTagColor(
        "Zenodo".equals(resource.resourceProvider()) ? TagColor.PRIMARY : TagColor.TEAL);
    cardHeader.add(providerTag);

    // Access badge
    var accessBadge = new Tag(
        resource.accessLevel() == AccessLevel.PUBLIC ? "Public" : "Restricted");
    accessBadge.setTagColor(
        resource.accessLevel() == AccessLevel.PUBLIC ? TagColor.SUCCESS : TagColor.WARNING);
    cardHeader.add(accessBadge);

    // Connected-by indicator (subtle)
    var connectedBySpan = new Span("by " + resource.connectedBy());
    connectedBySpan.addClassName("extra-small-body-text");
    connectedBySpan.addClassName("color-secondary");
    connectedBySpan.getStyle().set("margin-left", "auto");
    cardHeader.add(connectedBySpan);

    // Publication date (right-aligned)
    var dateSpan = new Span(resource.publicationDate().format(
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)));
    dateSpan.addClassName("extra-small-body-text");
    dateSpan.addClassName("color-secondary");
    cardHeader.add(dateSpan);

    card.add(cardHeader);

    // ── Card body: Title ──────────────────────────────────────────
    var body = new Div();
    body.getStyle().set("padding", "0 var(--lumo-space-m)");
    body.getStyle().set("flex-grow", "1");

    var titleSpan = new Span(resource.title());
    titleSpan.addClassName("normal-body-text");
    titleSpan.getStyle().set("font-weight", "600");
    titleSpan.getStyle().set("display", "block");
    titleSpan.getStyle().set("line-height", "1.4");
    titleSpan.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    body.add(titleSpan);

    // Update available hint (inline, below title)
    if (resource.updateAvailable()) {
      var updateHint = new Div();
      updateHint.addClassNames("flex-horizontal", "gap-02", "items-center");
      updateHint.getStyle().set("padding",
          "var(--lumo-space-xxs) var(--lumo-space-s)");
      updateHint.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
      updateHint.getStyle().set("background-color", "var(--lumo-warning-color-10pct)");
      updateHint.getStyle().set("margin-bottom", "var(--lumo-space-s)");
      updateHint.getStyle().set("cursor", "pointer");

      var warnIcon = VaadinIcon.EXCLAMATION_CIRCLE_O.create();
      warnIcon.getStyle().set("color", "var(--lumo-warning-color)");
      warnIcon.getStyle().set("font-size", "var(--lumo-icon-size-s)");

      var hintText = new Span("New version available");
      hintText.addClassName("extra-small-body-text");
      hintText.getStyle().set("color", "var(--lumo-warning-color)");
      hintText.getStyle().set("font-weight", "500");

      updateHint.add(warnIcon, hintText);
      updateHint.addClickListener(e -> syncSingleResource(resource));
      body.add(updateHint);
    }

    // ── Meta row: PID · Version · Access Link ─────────────────────
    var metaRow = new Div();
    metaRow.addClassNames("flex-horizontal", "gap-03", "items-center");
    metaRow.getStyle().set("flex-wrap", "wrap");
    metaRow.getStyle().set("margin-bottom", "var(--lumo-space-s)");

    // PID (as linked DOI)
    String pidHref = resource.pid().startsWith("http")
        ? resource.pid()
        : "https://doi.org/" + resource.pid();
    var pidLink = new Anchor(pidHref, resource.pid());
    pidLink.setTarget(AnchorTarget.BLANK);
    pidLink.addClassName("extra-small-body-text");
    metaRow.add(pidLink);

    // Separator
    var sep1 = new Span("·");
    sep1.addClassName("extra-small-body-text");
    sep1.addClassName("color-secondary");
    metaRow.add(sep1);

    // Version
    String versionDisplay = resource.version() != null && !resource.version().isBlank()
        ? resource.version()
        : "—";
    var versionSpan = new Span("v" + versionDisplay.replaceFirst("^v", ""));
    versionSpan.addClassName("extra-small-body-text");
    versionSpan.addClassName("color-secondary");
    metaRow.add(versionSpan);

    // Access link (if available)
    if (resource.accessLink() != null && !resource.accessLink().isBlank()) {
      var sep2 = new Span("·");
      sep2.addClassName("extra-small-body-text");
      sep2.addClassName("color-secondary");
      metaRow.add(sep2);

      var accessAnchor = new Anchor(resource.accessLink(), "Open ↗");
      accessAnchor.setTarget(AnchorTarget.BLANK);
      accessAnchor.addClassName("extra-small-body-text");
      metaRow.add(accessAnchor);
    }

    body.add(metaRow);

    // ── Access status detail (for restricted: show nuance) ──────
    if (resource.accessLevel() == AccessLevel.RESTRICTED) {
      var accessNote = new Span("Record: public · Files: restricted");
      accessNote.addClassName("extra-small-body-text");
      accessNote.addClassName("color-secondary");
      accessNote.getStyle().set("display", "block");
      accessNote.getStyle().set("margin-bottom", "var(--lumo-space-s)");
      accessNote.getStyle().set("font-style", "italic");
      body.add(accessNote);
    }

    // ── Separator before details ────────────────────────────────
    var detailsSep = new Hr();
    detailsSep.getStyle().set("margin", "0");
    detailsSep.getStyle().set("border-color", "var(--lumo-contrast-10pct)");
    body.add(detailsSep);

    // ── Expandable details section (medium-prio properties) ─────
    var detailsToggle = new Button("Details", e -> {
      var detailsPanel = card.getElement()
          .getChildren()
          .filter(c -> c.hasAttribute("data-details-panel"))
          .findFirst();
      detailsPanel.ifPresent(el -> {
        String current = el.getStyle().get("display");
        boolean isHidden = "none".equals(current);
        el.getStyle().set("display", isHidden ? "block" : "none");
        e.getSource().setText(isHidden ? "Hide details" : "Details");
      });
    });
    detailsToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    detailsToggle.getStyle().set("margin", "var(--lumo-space-xs) 0");
    body.add(detailsToggle);

    var detailsPanel = new Div();
    detailsPanel.getElement().setAttribute("data-details-panel", "true");
    detailsPanel.getStyle().set("display", "none");
    detailsPanel.addClassNames("flex-vertical", "gap-02");
    detailsPanel.getStyle().set("padding-bottom", "var(--lumo-space-s)");

    addDetailRow(detailsPanel, "Connected By", resource.connectedBy());
    addDetailRow(detailsPanel, "Resource Provider", resource.resourceProvider());
    addDetailRow(detailsPanel, "Creator", resource.creator());
    addDetailRow(detailsPanel, "Resource Type",
        resource.resourceType() != null ? resource.resourceType() : "—");
    addDetailRow(detailsPanel, "Community",
        resource.community() != null ? resource.community() : "—");
    addDetailRow(detailsPanel, "Linked Experiment",
        resource.linkedExperiment() != null ? resource.linkedExperiment() : "—");
    addDetailRow(detailsPanel, "Connected On",
        resource.connectedOn() != null
            ? resource.connectedOn().format(
                DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
            : "—");

    body.add(detailsPanel);
    card.add(body);

    // ── Card footer: Actions ────────────────────────────────────
    var footer = new Div();
    footer.addClassNames("flex-horizontal", "items-center");
    footer.getStyle().set("padding",
        "var(--lumo-space-s) var(--lumo-space-m)");
    footer.getStyle().set("border-top",
        "1px solid var(--lumo-contrast-10pct)");
    footer.getStyle().set("background-color", "var(--lumo-contrast-5pct)");

    // Spacer to push buttons right
    var spacer = new Div();
    spacer.getStyle().set("flex-grow", "1");
    footer.add(spacer);

    var syncBtn = new Button(VaadinIcon.REFRESH.create(),
        e -> syncSingleResource(resource));
    syncBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    syncBtn.getStyle().set("padding", "var(--lumo-space-s)");
    if (resource.updateAvailable()) {
      syncBtn.getStyle().set("color", "var(--lumo-warning-color)");
      syncBtn.setTooltipText("New version available on " + resource.resourceProvider()
          + ". Click to update this connection.");
    } else {
      syncBtn.setTooltipText("Check " + resource.resourceProvider() + " for updates.");
    }

    var removeBtn = new Button(VaadinIcon.TRASH.create(),
        e -> confirmRemoveResource(resource));
    removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
    removeBtn.setTooltipText("Remove connection");
    removeBtn.getStyle().set("padding", "var(--lumo-space-s)");

    footer.add(syncBtn, removeBtn);
    card.add(footer);

    return card;
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
    var valueSpan = new Span(value != null ? value : "—");
    valueSpan.addClassName("normal-body-text");
    row.add(labelSpan, valueSpan);
    container.add(row);
  }

  // ── Card refresh & filtering ──────────────────────────────────────

  private void refreshResourceCards() {
    cardsContainer.removeAll();
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

    if (filtered.isEmpty()) {
      cardsContainer.removeAll();
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
    wrapper.getStyle().set("grid-column", "1 / -1"); // span full grid width

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
            + "to this project so that you and your collaborators can keep "
            + "an overview of all associated data in one place.");
    explanation.addClassName("normal-body-text");
    explanation.addClassName("color-secondary");
    explanation.getStyle().set("max-width", "480px");
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
    long publicCount = connectedResources.stream()
        .filter(r -> r.accessLevel() == AccessLevel.PUBLIC).count();
    long restrictedCount = total - publicCount;
    if (resourceCountSpan != null) {
      resourceCountSpan.setText(
          "%d resource(s) — %d public, %d restricted".formatted(
              total, publicCount, restrictedCount));
    }
  }

  // ══════════════════════════════════════════════════════════════════════
  //  CONNECT SIDEBAR (reused from V2)
  // ══════════════════════════════════════════════════════════════════════

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

    // Sidebar panel
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

    var sidebarBody = new Div();
    sidebarBody.getStyle().set("height", "100%");
    sidebarBody.getStyle().set("box-sizing", "border-box");
    sidebarBody.getStyle().set("display", "flex");
    sidebarBody.getStyle().set("flex-direction", "column");

    // Header
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

    // Content area
    var contentArea = new Div();
    contentArea.getStyle().set("flex-grow", "1");
    contentArea.getStyle().set("overflow-y", "auto");
    contentArea.getStyle().set("padding", "var(--lumo-space-l)");

    // Info note
    var infoNote = new InfoBox()
        .setInfoText(
            "Search for datasets on InvenioRDM repositories and connect them to this project. "
                + "Both public and restricted datasets can be found here — access level is shown inline.")
        .setClosable(true);
    infoNote.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    contentArea.add(infoNote);

    // Search form
    var searchForm = new Div();
    searchForm.getStyle().set("margin-bottom", "var(--lumo-space-l)");

    instanceSelector.setItems(INVENIO_INSTANCES);
    instanceSelector.setPlaceholder("Select repository…");
    instanceSelector.setValue(INVENIO_INSTANCES.get(0));
    instanceSelector.setWidth("180px");
    instanceSelector.setLabel("Repository");
    instanceSelector.getStyle().set("flex-shrink", "0");
    instanceSelector.addClassName("connect-dataset-sidebar-overlay");
    instanceSelector.addValueChangeListener(e -> performSidebarSearch());

    searchField.setPlaceholder("Search by title, DOI, or creator…");
    searchField.setClearButtonVisible(true);
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

    // Search results grid
    searchResultsGrid = new Grid<>();
    searchResultsGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_ROW_BORDERS);
    searchResultsGrid.setSelectionMode(SelectionMode.MULTI);
    searchResultsGrid.setWidthFull();
    searchResultsGrid.setAllRowsVisible(true);

    searchResultsGrid.addComponentColumn(this::buildSearchResultCard)
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

    // Footer: Connect button
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

    sidebarPanel.add(sidebarBody);
    add(sidebarOverlay, sidebarPanel);
  }

  /**
   * Builds a single card-style row for the sidebar search results.
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

    Tag accessBadge = resource.accessLevel() == AccessLevel.PUBLIC
        ? new Tag("Public") : new Tag("Restricted");
    accessBadge.setTagColor(
        resource.accessLevel() == AccessLevel.PUBLIC ? TagColor.SUCCESS : TagColor.WARNING);
    topRow.add(accessBadge);

    var providerTag = new Tag(resource.resourceProvider());
    providerTag.setTagColor(
        "Zenodo".equals(resource.resourceProvider()) ? TagColor.PRIMARY : TagColor.TEAL);
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

    var doiAnchor = new Anchor(
        resource.pid().startsWith("http") ? resource.pid()
            : "https://doi.org/" + resource.pid(),
        resource.pid());
    doiAnchor.setTarget(AnchorTarget.BLANK);
    doiAnchor.addClassName("extra-small-body-text");
    metaRow.add(doiAnchor);

    card.add(metaRow);
    return card;
  }

  private void openConnectSidebar() {
    sidebarOpen = true;
    sidebarOverlay.getStyle().set("display", "block");
    sidebarPanel.getStyle().set("display", "block");
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
  //  SEARCH & FILTER
  // ══════════════════════════════════════════════════════════════════════

  private void performSidebarSearch() {
    String selectedRepo = instanceSelector.getValue();
    String term = searchField.getValue();

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

  private boolean matchesRepository(SearchableResource resource, String repository) {
    if (repository == null || repository.isBlank()) {
      return true;
    }
    return resource.resourceProvider().equals(extractProviderName(repository));
  }

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
          null,
          sr.id(), LocalDate.now(), false
      ));
    }
    refreshResourceCards();
    searchResultsGrid.deselectAll();
    closeConnectSidebar();
    showSuccessNotification(selected.size()
        + " dataset(s) connected to this project.");
  }

  private void confirmRemoveResource(ConnectedResource resource) {
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
      refreshResourceCards();
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
      ui.access(() -> showInfoNotification("Sync complete for '" + resource.title() + "'."));
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
      ui.access(() -> showSuccessNotification("All " + connectedResources.size()
          + " resources are up to date."));
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

  private void showErrorNotification(String message) {
    var notification = new Notification(message, 4000);
    notification.addClassName("error-toast");
    notification.setPosition(Notification.Position.BOTTOM_END);
    notification.open();
  }

  // ══════════════════════════════════════════════════════════════════════
  //  REPOSITORY CREDENTIALS SECTION (unchanged from V2)
  // ══════════════════════════════════════════════════════════════════════

  private Div credentialsContentContainer;

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

  private void refreshCredentialsContent() {
    credentialsContentContainer.removeAll();

    // Benefits callout
    var benefitsCallout = new Div();
    benefitsCallout.addClassNames("border", "rounded-02");
    benefitsCallout.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    benefitsCallout.getStyle().set("background-color", "var(--lumo-primary-color-10pct)");
    benefitsCallout.getStyle().set("border-color", "var(--lumo-primary-color-30pct)");
    benefitsCallout.getStyle().set("margin-bottom", "var(--lumo-space-s)");

    var benefitsHeader = new Div();
    benefitsHeader.addClassNames("flex-horizontal", "gap-02", "items-center");
    var benefitsIcon = VaadinIcon.LIGHTBULB.create();
    benefitsIcon.getStyle().set("color", "var(--lumo-primary-color)");
    var benefitsTitle = new Span("Why configure repository access?");
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

    for (var instance : AVAILABLE_INSTANCES) {
      credentialsContentContainer.add(buildInstanceCard(instance));
    }
  }

  private Div buildInstanceCard(InvenioRDMInstance instance) {
    var card = new Div();
    card.addClassNames("border", "rounded-02");
    card.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");

    boolean isConfigured = configuredCredentials.containsKey(instance.id());

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

    var descSpan = new Span(instance.description());
    descSpan.addClassName("extra-small-body-text");
    descSpan.addClassName("color-secondary");
    descSpan.getStyle().set("display", "block");
    descSpan.getStyle().set("margin-bottom", "var(--lumo-space-s)");
    card.add(descSpan);

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

      var removeButton = new Button("Remove", VaadinIcon.TRASH.create());
      removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
      removeButton.setTooltipText("Remove the stored token for " + instance.shortName());
      removeButton.addClickListener(e -> openRemoveTokenDialog(instance));
      bottomRow.add(detailsWrapper, removeButton);
    } else {
      var hintText = new Span("No personal access token configured for this repository.");
      hintText.addClassName("extra-small-body-text");
      hintText.addClassName("color-secondary");
      hintText.getStyle().set("flex-grow", "1");

      var configureButton = new Button("Configure", VaadinIcon.PLUS_CIRCLE.create());
      configureButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      configureButton.setTooltipText("Add a personal access token to connect restricted datasets.");
      configureButton.addClickListener(e -> openAddTokenDialog(instance));
      bottomRow.add(hintText, configureButton);
    }
    card.add(bottomRow);

    return card;
  }

  // ── Add Token Dialog ────────────────────────────────────────────────

  private void openAddTokenDialog(InvenioRDMInstance instance) {
    var dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setHeaderTitle("Connect " + instance.displayName());
    dialog.setWidth("520px");
    dialog.setCloseOnOutsideClick(false);

    var content = new Div();
    content.addClassNames("flex-vertical", "gap-03");
    content.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");

    var introText = new Div();
    introText.addClassName("normal-body-text");
    introText.getElement().setProperty("innerHTML",
        "To connect access-restricted datasets from <b>" + instance.shortName()
            + "</b>, provide a Personal Access Token from your "
            + instance.shortName() + " account.");
    content.add(introText);

    var tokenField = new PasswordField();
    tokenField.setLabel("Personal Access Token");
    tokenField.setPlaceholder("Paste your token here…");
    tokenField.setWidthFull();
    tokenField.setRequiredIndicatorVisible(true);
    tokenField.setHelperText("Generate a token in your " + instance.shortName()
        + " account settings.");
    content.add(tokenField);

    var tokenSetupLink = new Anchor(instance.tokenSetupUrl(),
        "Open " + instance.shortName() + " token settings \u2192");
    tokenSetupLink.setTarget(AnchorTarget.BLANK);
    tokenSetupLink.addClassName("extra-small-body-text");
    content.add(tokenSetupLink);

    var statusArea = new Div();
    statusArea.getStyle().set("display", "none");
    statusArea.getStyle().set("padding", "var(--lumo-space-s)");
    statusArea.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
    content.add(statusArea);

    var validationHint = new Div();
    validationHint.addClassName("extra-small-body-text");
    validationHint.addClassName("color-secondary");
    validationHint.getStyle().set("font-style", "italic");
    validationHint.setText("In this prototype, any non-empty token is accepted.");
    content.add(validationHint);

    dialog.add(content);

    Button connectBtn = new Button("Validate & Connect");
    connectBtn.addClickListener(e -> {
      String tokenValue = tokenField.getValue();
      if (tokenValue == null || tokenValue.isBlank()) {
        statusArea.removeAll();
        statusArea.getStyle().set("display", "block");
        statusArea.getStyle().set("background-color", "var(--lumo-error-color-10pct)");
        statusArea.getStyle().set("border", "1px solid var(--lumo-error-color)");
        var errRow = new Div();
        errRow.addClassNames("flex-horizontal", "gap-02", "items-center");
        var errIcon = VaadinIcon.WARNING.create();
        errIcon.getStyle().set("color", "var(--lumo-error-color)");
        var errText = new Span("Token cannot be empty.");
        errText.getStyle().set("color", "var(--lumo-error-color)");
        errRow.add(errIcon, errText);
        statusArea.add(errRow);
        return;
      }

      var ui = UI.getCurrent();
      connectBtn.setEnabled(false);
      connectBtn.setText("Validating\u2026");

      dialog.getClassNames().add("connect-dataset-sidebar-dialog");
      new Thread(() -> {
        try {
          Thread.sleep(1200);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
        ui.access(() -> {
          String masked = maskToken(tokenValue);
          configuredCredentials.put(instance.id(),
              new RepositoryCredential(instance.id(), masked, LocalDate.now()));
          dialog.close();
          refreshCredentialsContent();
          showSuccessNotification("Token connected successfully for "
              + instance.shortName() + ".");
        });
      }).start();
    });
    connectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    var cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    dialog.getFooter().add(cancelBtn, connectBtn);
    dialog.open();
    tokenField.focus();
  }

  private String maskToken(String token) {
    if (token == null || token.length() <= 8) {
      return "****";
    }
    return token.substring(0, 4) + "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
        + token.substring(token.length() - 4);
  }

  // ── Remove Token Dialog ─────────────────────────────────────────────

  private void openRemoveTokenDialog(InvenioRDMInstance instance) {
    var dialog = new com.vaadin.flow.component.dialog.Dialog();
    dialog.setCloseOnOutsideClick(false);
    dialog.setCloseOnEsc(false);
    dialog.setWidth("480px");

    var body = new Div();
    body.addClassNames("flex-vertical", "gap-03");
    body.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");

    var headerRow = new Div();
    headerRow.addClassNames("flex-horizontal", "gap-03", "items-center");
    var warnIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
    warnIcon.addClassName("icon-color-warning");
    var titleSpan = new Span("Remove Repository Connection");
    titleSpan.addClassName("heading-3");
    headerRow.add(warnIcon, titleSpan);
    body.add(headerRow);

    var confirmText = new Div();
    confirmText.addClassName("normal-body-text");
    confirmText.getElement().setProperty("innerHTML",
        "You are about to remove the Personal Access Token for "
            + "<b>" + instance.displayName() + "</b>.");
    body.add(confirmText);

    body.add(new Span(
        "You can reconfigure this repository at any time by adding a new token."));

    dialog.add(body);

    var removeBtn = new Button("Remove Token", VaadinIcon.TRASH.create(), e -> {
      configuredCredentials.remove(instance.id());
      dialog.close();
      refreshCredentialsContent();
      showSuccessNotification("Repository connection removed for "
          + instance.shortName() + ".");
    });
    removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

    var cancelBtn = new Button("Cancel", e -> dialog.close());
    cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    dialog.getFooter().add(cancelBtn, removeBtn);
    dialog.open();
  }

  // ── Seed data ───────────────────────────────────────────────────────

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
            "a1b2\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022c3d4",
            LocalDate.of(2025, 3, 10)));
  }
}
