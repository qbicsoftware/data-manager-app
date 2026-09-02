package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.projects.project.datasets.ConnectedDatasetsMain;
import org.springframework.context.annotation.Profile;

/**
 * <b>Project Listing with Datasets — Demo View</b>
 *
 * <p>Prototype UI for story FEAT-DATSET-09: "Access available datasets after login".
 * Enhances the existing project collection listing with connected-resource indicators
 * per project card, so researchers can see at a glance which projects have associated
 * datasets, how many, and their access status (open vs. restricted).</p>
 *
 * <h3>Design rationale</h3>
 * <ul>
 *   <li><b>Faithful to the existing layout</b> — the project cards retain their
 *       current structure (title, tags, dates, PI, avatars). The dataset
 *       indicator is an additive element placed below the existing meta info.</li>
 *   <li><b>Colour-coded access status</b> — the indicator uses the existing
 *       {@link life.qbic.datamanager.views.general.Tag} palette:
 *       <ul>
 *         <li>Green (success) — all connected datasets are open/public</li>
 *         <li>Amber (warning) — at least one connected dataset is access-restricted</li>
 *         <li>Grey (contrast/neutral) — no datasets connected</li>
 *       </ul>
 *   <li><b>Two distinct click targets per card</b> — clicking anywhere on the
 *       card body (title, PI, avatars) navigates to the project info page;
 *       clicking the dataset indicator row navigates directly to the
 *       project's connected-datasets view ({@code projects/%s/datasets}).
 *       The indicator is underlined on hover and carries a trailing
 *       {@code →} icon as affordance.</li>
 *   <li><b>Summary ribbon</b> — above the project cards, a compact summary
 *       shows aggregate statistics across all projects (total connected
 *       datasets, projects with connections, open/restricted breakdown).</li>
 * </ul>
 *
 * <p>This view is only available with the {@code development} profile.</p>
 *
 * @since 1.12.0
 */
@Profile("development")
@Route("test-view/project-listing-datasets")
@UIScope
@AnonymousAllowed
@org.springframework.stereotype.Component
public class ProjectListingDatasetsDemo extends Div {

  // ── Domain records ────────────────────────────────────────────────────

  /** Access level of a connected dataset (mirrors FEAT-DATASET-CONNECTION). */
  enum AccessLevel { OPEN, RESTRICTED }

  /** A dataset connection within a project. */
  record DatasetConnection(
      String id,
      String title,
      String pid,
      AccessLevel accessLevel,
      String repository,
      String linkedExperimentName,
      LocalDate connectedOn
  ) {}

  /** A project overview enriched with connected-dataset summary. */
  record ProjectOverviewWithDatasets(
      String projectId,
      String projectCode,
      String projectTitle,
      Instant lastModified,
      String principalInvestigator,
      String projectResponsible,
      List<MeasurementKind> measurementTypes,
      int collaboratorCount,
      List<DatasetConnection> connectedDatasets
  ) {}

  /** Simplified measurement kind for display. */
  record MeasurementKind(String label, String cssClass) {}

  // ── Mock project data ─────────────────────────────────────────────────

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy HH:mm:ss", Locale.ENGLISH)
          .withZone(ZoneId.of("Europe/Berlin"));

  private static final List<ProjectOverviewWithDatasets> MOCK_PROJECTS = seedProjects();

  private TextField searchField;
  private Div projectCardsContainer;
  private Span summaryText;

  public ProjectListingDatasetsDemo() {
    addClassName("padding-horizontal-07");
    addClassName("padding-vertical-04");

    // ── Page title ─────────────────────────────────────────────────
    var pageTitle = new Div("Welcome Back admin-test!");
    pageTitle.addClassName("project-overview-title");
    add(pageTitle);

    var pageDescription = new Div(
        "Manage all your scientific data in one place with the Data Manager. "
            + "You can access our documentation and learn more about using the Data Manager. "
            + "Start by creating a new project or continue working on an already existing project.");
    pageDescription.addClassName("description");
    add(pageDescription);

    // ── Subtitle explaining this is a prototype ────────────────────
    var protoNotice = new Div();
    protoNotice.getStyle().set("display", "flex");
    protoNotice.getStyle().set("align-items", "center");
    protoNotice.getStyle().set("gap", "var(--lumo-space-s)");
    protoNotice.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    protoNotice.getStyle().set("margin-top", "var(--lumo-space-m)");

    var infoIcon = VaadinIcon.INFO_CIRCLE_O.create();
    infoIcon.getStyle().set("color", "var(--lumo-primary-color)");
    infoIcon.getStyle().set("flex-shrink", "0");

    var noticeText = new Span(
        "Prototype: FEAT-DATSET-09 — connected-dataset indicators on project cards (no backend wire-up).");
    noticeText.addClassName("extra-small-body-text");
    noticeText.addClassName("color-secondary");

    protoNotice.add(infoIcon, noticeText);
    add(protoNotice);

    // ── Summary ribbon ─────────────────────────────────────────────
    add(buildSummaryRibbon());

    // Wrap header + cards in project-collection-component for proper CSS scoping
    var layoutWrapper = new Div();
    layoutWrapper.addClassName("project-collection-component");

    // ── Header + project cards container ──────────────────────────
    layoutWrapper.add(buildMyProjectsHeader());

    projectCardsContainer = new Div();
    projectCardsContainer.addClassName("project-cards-container");
    projectCardsContainer.getStyle().set("display", "flex");
    projectCardsContainer.getStyle().set("flex-direction", "column");
    projectCardsContainer.getStyle().set("gap", "0");

    refreshProjectCards(MOCK_PROJECTS);
    layoutWrapper.add(projectCardsContainer);
    add(layoutWrapper);
  }

  // ══════════════════════════════════════════════════════════════════════
  //  HEADER
  // ══════════════════════════════════════════════════════════════════════

  private Div buildMyProjectsHeader() {
    var header = new Div();
    header.addClassName("header");

    var title = new Span("My Projects");
    title.addClassName("title");

    searchField = new TextField();
    searchField.setPlaceholder("Search");
    searchField.setClearButtonVisible(true);
    searchField.setSuffixComponent(VaadinIcon.SEARCH.create());
    searchField.addClassNames("search-field");
    searchField.setValueChangeMode(ValueChangeMode.LAZY);
    searchField.addValueChangeListener(e -> filterProjects(e.getValue()));

    var createBtn = new Button("Create");
    createBtn.addClassName("primary");

    var controls = new Div(searchField, createBtn);
    controls.addClassName("controls");

    header.add(title, controls);
    return header;
  }

  // ══════════════════════════════════════════════════════════════════════
  //  SUMMARY RIBBON
  // ══════════════════════════════════════════════════════════════════════

  private Div buildSummaryRibbon() {
    var ribbon = new Div();
    ribbon.addClassName("border");
    ribbon.addClassName("rounded-02");
    ribbon.getStyle().set("display", "flex");
    ribbon.getStyle().set("align-items", "center");
    ribbon.getStyle().set("gap", "var(--lumo-space-l)");
    ribbon.getStyle().set("padding", "var(--lumo-space-m) var(--lumo-space-l)");
    ribbon.getStyle().set("margin-bottom", "var(--lumo-space-m)");
    ribbon.getStyle().set("background-color", "var(--lumo-base-color)");
    ribbon.getStyle().set("overflow-x", "auto");

    var totalProjects = MOCK_PROJECTS.size();
    var projectsWithDatasets = MOCK_PROJECTS.stream()
        .filter(p -> !p.connectedDatasets().isEmpty()).count();
    var totalDatasets = MOCK_PROJECTS.stream()
        .mapToInt(p -> p.connectedDatasets().size()).sum();
    var openDatasets = MOCK_PROJECTS.stream()
        .flatMap(p -> p.connectedDatasets().stream())
        .filter(d -> d.accessLevel() == AccessLevel.OPEN).count();
    var restrictedDatasets = totalDatasets - openDatasets;

    summaryText = new Span("%d project(s) · %d with connected datasets · %d dataset(s) total (%d open, %d restricted)"
        .formatted(totalProjects, projectsWithDatasets, totalDatasets, openDatasets, restrictedDatasets));
    summaryText.addClassName("normal-body-text");

    var dbIcon = VaadinIcon.DATABASE.create();
    dbIcon.getStyle().set("color", "var(--lumo-primary-color)");
    dbIcon.getStyle().set("font-size", "var(--lumo-icon-size-m)");
    dbIcon.getStyle().set("flex-shrink", "0");

    ribbon.add(dbIcon, summaryText);
    return ribbon;
  }

  // ══════════════════════════════════════════════════════════════════════
  //  PROJECT CARD (matching existing ProjectOverviewItem layout)
  // ══════════════════════════════════════════════════════════════════════

  private Component buildProjectCard(ProjectOverviewWithDatasets project) {
    var card = new Div();
    card.addClassName("project-overview-item");
    card.getStyle().set("cursor", "pointer");

    // ── Header: title + measurement tags ───────────────────────────
    var header = new Div();
    header.addClassName("flex-horizontal");
    header.getStyle().set("align-items", "flex-start");
    header.getStyle().set("gap", "var(--lumo-space-s)");
    header.getStyle().set("flex-wrap", "wrap");

    var titleSpan = new Span("%s - %s".formatted(project.projectCode(), project.projectTitle()));
    titleSpan.addClassName("project-overview-item-title");
    header.add(titleSpan);

    var tagsContainer = new Div();
    tagsContainer.addClassName("tag-collection");
    for (var kind : project.measurementTypes()) {
      var tag = new Span(kind.label());
      tag.addClassName("tag");
      tag.addClassName(kind.cssClass());
      tagsContainer.add(tag);
    }
    header.add(tagsContainer);

    card.add(header);

    // ── Last modified ─────────────────────────────────────────────
    var lastModified = new Span("Last modified on " + DATE_FMT.format(project.lastModified()));
    lastModified.addClassName("tertiary");
    card.add(lastModified);

    // ── PI + Responsible ───────────────────────────────────────────
    var details = new Div();
    details.addClassName("details");
    details.add(new Span("Principal Investigator: " + project.principalInvestigator()));
    if (project.projectResponsible() != null && !project.projectResponsible().isBlank()) {
      details.add(new Span("Project Responsible: " + project.projectResponsible()));
    }
    card.add(details);

    // ── Collaborator avatars (simplified: show count + placeholder) ─
    var avatarPlaceholder = buildAvatarPlaceholder(project.collaboratorCount());
    card.add(avatarPlaceholder);

    // ── Connected Datasets footer (RouterLink → datasets view) ─
    // Differentiated footer region at the bottom of the card. Entire
    // footer area is a RouterLink → projects/{id}/datasets; the rest
    // of the card stays as a project-info link.
    if (!project.connectedDatasets().isEmpty()) {
      card.add(buildDatasetFooter(project));
    }

    // ── Click handler: project card body → project info ─────────
    card.addClickListener(event -> {
      navigateToProjectInfo(project.projectId());
    });

    return card;
  }
  // ══════════════════════════════════════════════════════════════════════
  //  DATASET FOOTER — RouterLink wrapping full footer region
  // ══════════════════════════════════════════════════════════════════════

  /**
   * Builds the connected-datasets footer region for a project card.
   *
   * <h3>Why a footer, not just a chevron icon?</h3>
   * <p>A small icon is too tiny a click target for users with motor
   * disabilities and problematic for screen readers. By differentiating
   * the dataset section as a full-width card footer — with a top border,
   * a slightly tinted background, and a {@link RouterLink} wrapping
   * meaningful content — the full footer becomes a large (44px+ high) 
   * click target that is easy to hit and carries coherent link text.</p>
   *
   * <h3>Interaction model</h3>
   * <ul>
   *   <li><b>Footer area</b> (border to card bottom) → RouterLink
   *       navigates to {@code projects/{id}/datasets}. Anchor navigation
   *       is handled by the browser before Vaadin's server-side click
   *       routing, so the parent card project-info handler does not
   *       fire.</li>
   *   <li><b>Card body</b> (title, PI, avatars) → card-wide
   *       {@code Div.addClickListener(...)} navigates to project info.
   *   </li>
   * </ul>
   *
   * <h3>Accessibility</h3>
   * <ul>
   *   <li>Full-width, 44px+ vertical click target — meets minimum
   *       touch-target guidelines.</li>
   *   <li>Link wraps meaningful content (count, Open/Restricted Tags,
   *       last connected date); screen readers get a coherent sentence
   *       instead of an icon name.</li>
   *   <li>{@code aria-label}: {@code "Open datasets for Q2KX4B: 3
   *       connected, 2 open, 1 restricted, last updated 20 July 2026"}
   *       — unambiguous without visual context.</li>
   *   <li>Visual boundary (top border + tinted background) signals a
   *       different interaction zone.</li>
   *   <li>{@code :focus-visible} outline for keyboard navigation.</li>
   * </ul>
   *
   * <h3>Layout</h3>
   * <ol>
   *   <li>Database icon (neutral, secondary colour)</li>
   *   <li>Dataset count (bold) + "dataset"/"datasets" label</li>
   *   <li>{@code ·} separator</li>
   *   <li>{@code Tag("n Open", SUCCESS)}</li>
   *   <li>{@code Tag("n Restricted", WARNING)}</li>
   *   <li>{@code ·} separator</li>
   *   <li>"Last connected dd MMM yyyy"</li>
   *   <li>Spacer (flex-grow)</li>
   *   <li>Trailing chevron ({@link VaadinIcon#CHEVRON_RIGHT}) — visual cue</li>
   * </ol>
   */
  private Component buildDatasetFooter(ProjectOverviewWithDatasets project) {
    var content = new Div();
    content.getStyle().set("display", "flex");
    content.getStyle().set("align-items", "center");
    content.getStyle().set("gap", "var(--lumo-space-s)");
    content.getStyle().set("flex-wrap", "wrap");

    var openCount = project.connectedDatasets().stream()
        .filter(d -> d.accessLevel() == AccessLevel.OPEN).count();
    long restrictedCount = project.connectedDatasets().size() - openCount;

    var icon = VaadinIcon.DATABASE.create();
    icon.getStyle().set("font-size", "var(--lumo-icon-size-s)");
    icon.getStyle().set("flex-shrink", "0");
    icon.addClassName("color-secondary");
    content.add(icon);

    var countSpan = new Span(String.valueOf(project.connectedDatasets().size()));
    countSpan.getStyle().set("font-weight", "700");
    countSpan.addClassName("normal-body-text");
    content.add(countSpan);

    var labelSpan = new Span(project.connectedDatasets().size() == 1 ? "dataset" : "datasets");
    labelSpan.addClassName("extra-small-body-text");
    content.add(labelSpan);

    content.add(buildDotSeparator());

    if (openCount > 0) {
      var openTag = new Tag("%d Open".formatted(openCount));
      openTag.setTagColor(TagColor.SUCCESS);
      content.add(openTag);
    }
    if (restrictedCount > 0) {
      var restrictedTag = new Tag("%d Restricted".formatted(restrictedCount));
      restrictedTag.setTagColor(TagColor.WARNING);
      content.add(restrictedTag);
    }

    content.add(buildDotSeparator());

    var lastConnected = project.connectedDatasets().stream()
        .map(DatasetConnection::connectedOn)
        .max(Comparator.naturalOrder())
        .orElse(null);

    var lastConnectedLabel = new Span("Last connected");
    lastConnectedLabel.addClassName("extra-small-body-text");
    lastConnectedLabel.addClassName("color-secondary");
    content.add(lastConnectedLabel);

    if (lastConnected != null) {
      var lastConnectedDate = new Span(lastConnected.format(
          DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)));
      lastConnectedDate.addClassName("extra-small-body-text");
      content.add(lastConnectedDate);
    } else {
      var fallback = new Span("—");
      fallback.addClassName("extra-small-body-text");
      fallback.addClassName("color-secondary");
      content.add(fallback);
    }

    // Spacer to push the chevron to the right edge
    var spacer = new Div();
    spacer.getStyle().set("flex-grow", "1");
    content.add(spacer);

    // Trailing chevron — visual cue only; the whole footer is clickable
    var chevron = VaadinIcon.CHEVRON_RIGHT.create();
    chevron.getStyle().set("font-size", "var(--lumo-icon-size-s)");
    chevron.getStyle().set("flex-shrink", "0");
    content.add(chevron);

    // ─ Wrap in RouterLink — entire footer is the link ──────────
    var footerLink = new RouterLink(
        "", ConnectedDatasetsMain.class,
        new RouteParameters("projectId", project.projectId()));
    footerLink.addClassName("project-dataset-footer");
    footerLink.add(content);

    // aria-label — meaningful text for screen readers
    String ariaText = "Open datasets for %s: %d connected, %d open, %d restricted".formatted(
        project.projectCode(),
        project.connectedDatasets().size(),
        openCount,
        restrictedCount);
    if (lastConnected != null) {
      ariaText += ", last updated %s".formatted(lastConnected.format(
          DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)));
    }
    footerLink.getElement().setAttribute("aria-label", ariaText);

    return footerLink;
  }


  private Div buildAvatarPlaceholder(int count) {
    var wrapper = new Div();
    wrapper.addClassName("flex-horizontal");
    wrapper.getStyle().set("gap", "var(--lumo-space-0)");
    wrapper.getStyle().set("align-items", "center");
    wrapper.getStyle().set("margin-top", "var(--lumo-space-xs)");

    // Simple colored circle placeholders (no real avatar service in prototype)
    int show = Math.min(count, 5);
    var colors = List.of("#5A9BD5", "#58C9B9", "#FFB627", "#E8637A", "#9B6BCD");
    for (int i = 0; i < show; i++) {
      var circle = new Div();
      circle.getStyle().set("width", "28px");
      circle.getStyle().set("height", "28px");
      circle.getStyle().set("border-radius", "50%");
      circle.getStyle().set("background-color", colors.get(i % colors.size()));
      circle.getStyle().set("border", "2px solid var(--lumo-base-color)");
      circle.getStyle().set("margin-left", i > 0 ? "-8px" : "0");
      circle.getStyle().set("display", "flex");
      circle.getStyle().set("align-items", "center");
      circle.getStyle().set("justify-content", "center");
      circle.getStyle().set("color", "white");
      circle.getStyle().set("font-size", "10px");
      circle.getStyle().set("font-weight", "600");
      circle.setText(String.valueOf((char) ('A' + i)));
      wrapper.add(circle);
    }
    if (count > 5) {
      var more = new Div();
      more.getStyle().set("width", "28px");
      more.getStyle().set("height", "28px");
      more.getStyle().set("border-radius", "50%");
      more.getStyle().set("background-color", "var(--lumo-contrast-20pct)");
      more.getStyle().set("border", "2px solid var(--lumo-base-color)");
      more.getStyle().set("margin-left", "-8px");
      more.getStyle().set("display", "flex");
      more.getStyle().set("align-items", "center");
      more.getStyle().set("justify-content", "center");
      more.getStyle().set("color", "var(--lumo-contrast)");
      more.getStyle().set("font-size", "10px");
      more.getStyle().set("font-weight", "600");
      more.setText("+" + (count - 5));
      wrapper.add(more);
    }
    return wrapper;
  }

  private Span buildDotSeparator() {
    var dot = new Span("·");
    dot.addClassName("extra-small-body-text");
    dot.addClassName("color-secondary");
    return dot;
  }

  /**
   * Navigate from a project card body click to the project's info page.
   *
   * <p>In the prototype this is a toast — the real implementation uses
   * {@code UI.navigate(ProjectInformationMain.class, RouteParam(...))}.</p>
   *
   * <p>Intentionally NOT fired when the user clicks the dataset indicator:
   * the chevron inside the indicator row is a {@link RouterLink} (native {@code <a>}), and anchor
   * navigation short-circuits Vaadin's server-side click routing on the
   * parent card element.</p>
   */
  private void navigateToProjectInfo(String projectId) {
    var ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    showToast("Navigate to project info — projects/" + projectId + "/info");
  }

  private void showToast(String message) {
    var notification = new com.vaadin.flow.component.notification.Notification(
        new Div(message));
    notification.setPosition(com.vaadin.flow.component.notification.Notification.Position.BOTTOM_END);
    notification.setDuration(4000);
    notification.addClassName("info-toast");
    notification.open();
  }

  private void filterProjects(String query) {
    List<ProjectOverviewWithDatasets> filtered;
    if (query == null || query.isBlank()) {
      filtered = new ArrayList<>(MOCK_PROJECTS);
    } else {
      String lower = query.toLowerCase();
      filtered = MOCK_PROJECTS.stream()
          .filter(p -> p.projectTitle().toLowerCase().contains(lower)
              || p.projectCode().toLowerCase().contains(lower)
              || p.principalInvestigator().toLowerCase().contains(lower))
          .toList();
    }
    refreshProjectCards(filtered);
  }

  private void refreshProjectCards(List<ProjectOverviewWithDatasets> projects) {
    projectCardsContainer.removeAll();
    for (ProjectOverviewWithDatasets project : projects) {
      projectCardsContainer.add(buildProjectCard(project));
    }
  }

  // ══════════════════════════════════════════════════════════════════════
  //  MOCK DATA
  // ══════════════════════════════════════════════════════════════════════

  private static List<ProjectOverviewWithDatasets> seedProjects() {
    var projects = new ArrayList<ProjectOverviewWithDatasets>();

    projects.add(new ProjectOverviewWithDatasets(
        "Q290MB", "Q290MB", "A nice test for spring boot 3.5.16",
        Instant.parse("2026-07-24T06:29:22Z"),
        "Tobias Koch", null,
        List.of(new MeasurementKind("Genomics", "pink")),
        2,
        List.of()
    ));

    projects.add(new ProjectOverviewWithDatasets(
        "Q2KX4B", "Q2KX4B", "Vaadin 24 Update Test 2",
        Instant.parse("2026-07-20T12:09:52Z"),
        "Steffen Greiner", "Sven Admin Istrator",
        List.of(new MeasurementKind("Proteomics", "violet")),
        3,
        List.of(
            new DatasetConnection("ds-001", "Cryo-EM structure of human 26S proteasome",
                "10.5281/zenodo.1234567", AccessLevel.OPEN, "Zenodo",
                "Main experiment", LocalDate.of(2026, 7, 15)),
            new DatasetConnection("ds-002",
                "Proteomic profiling of T-cell receptor signaling",
                "10.5281/zenodo.1234568", AccessLevel.OPEN, "Zenodo",
                "TCR experiment", LocalDate.of(2026, 7, 18)),
            new DatasetConnection("ds-003",
                "Clinical phosphoproteomics — Cohort B (embargo)",
                "10.5281/fdat.5554443", AccessLevel.RESTRICTED, "FDAT",
                "Main experiment", LocalDate.of(2026, 7, 20))
        )
    ));

    projects.add(new ProjectOverviewWithDatasets(
        "Q27QUE", "Q27QUE", "Test Ke Wang",
        Instant.parse("2026-07-09T09:12:56Z"),
        "Ke Wang", null,
        List.of(new MeasurementKind("Proteomics", "violet")),
        1,
        List.of()
    ));

    projects.add(new ProjectOverviewWithDatasets(
        "Q28ABZ", "Q28ABZ", "Arabidopsis drought stress multi-omics",
        Instant.parse("2026-07-15T14:03:11Z"),
        "Heike Weber", "QBiC Steward",
        List.of(
            new MeasurementKind("Genomics", "pink"),
            new MeasurementKind("Proteomics", "violet")
        ),
        5,
        List.of(
            new DatasetConnection("ds-010",
                "NGS raw reads: Arabidopsis thaliana drought stress response",
                "10.5281/zenodo.1234571", AccessLevel.OPEN, "Zenodo",
                "Drought stress RNA-Seq", LocalDate.of(2026, 7, 10)),
            new DatasetConnection("ds-011",
                "Spatiotemporal proteome of Arabidopsis root tissue",
                "10.5281/zenodo.1234599", AccessLevel.OPEN, "Zenodo",
                "Root proteomics", LocalDate.of(2026, 7, 14))
        )
    ));

    projects.add(new ProjectOverviewWithDatasets(
        "Q29CDE", "Q29CDE", "Spatial transcriptomics atlas — human kidney",
        Instant.parse("2026-07-01T09:00:00Z"),
        "F. Rossi", null,
        List.of(new MeasurementKind("Genomics", "pink")),
        4,
        List.of(
            new DatasetConnection("ds-020",
                "Spatial transcriptomics atlas of human kidney development",
                "10.5281/fdat.9876544", AccessLevel.OPEN, "FDAT",
                "Atlas experiment", LocalDate.of(2026, 6, 28)),
            new DatasetConnection("ds-021",
                "Pre-publication validation cohort (controlled access)",
                "10.5281/zenodo.3345501", AccessLevel.RESTRICTED, "Zenodo", null,
                LocalDate.of(2026, 6, 30)),
            new DatasetConnection("ds-022",
                "Companion proteomics dataset",
                "10.5281/fdat.9876600", AccessLevel.OPEN, "FDAT",
                "Atlas experiment", LocalDate.of(2026, 7, 1)),
            new DatasetConnection("ds-023",
                "Ethics-board restricted imaging data",
                "10.5281/fdat.9876700", AccessLevel.RESTRICTED, "FDAT", null,
                LocalDate.of(2026, 7, 5))
        )
    ));

    projects.add(new ProjectOverviewWithDatasets(
        "Q25XYZ", "Q25XYZ", "Lipidomics reference study",
        Instant.parse("2026-06-10T11:22:33Z"),
        "P. Garcia", "QBiC Manager",
        List.of(new MeasurementKind("Immunopeptidomics", "gold")),
        7,
        List.of(
            new DatasetConnection("ds-030",
                "Lipidomics reference panel — Phase I",
                "10.5281/zenodo.4455667", AccessLevel.OPEN, "Zenodo",
                "Phase I validation", LocalDate.of(2026, 6, 1))
        )
    ));

    return projects;
  }
}
