package life.qbic.datamanager.views.projects.project.datasets;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;
import life.qbic.projectmanagement.domain.model.associated_dataset.AccessLevel;

/**
 * <b>Connected Resources Component</b>
 *
 * <p>Renders the "Connected Resources" section within the associated
 * datasets view. Contains:
 * <ul>
 *   <li>an action bar with a primary "Connect Datasets" button (and a
 *       "Sync All" button reserved for story 04, currently disabled);</li>
 *   <li>a grid of connected datasets when at least one is present, or an
 *       empty-state guidance panel when no datasets are connected yet.</li>
 * </ul>
 *
 * <p>The grid consumes {@link ConnectedDatasetView} DTOs from the
 * application layer (never domain entities directly). User and experiment
 * display names are already resolved by the service — this component
 * renders them as-is.</p>
 *
 * <p>The component fires a {@link ConnectDatasetsClickEvent} whenever the
 * "Connect Datasets" button is clicked (from either the action bar or the
 * empty-state CTA), delegating the sidebar open/close handling to the
 * parent view.</p>
 *
 * @since 1.12.0
 */
public class ConnectedResourcesComponent extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

  private final AssociatedDatasetService associatedDatasetService;
  private final Section section;
  private final Grid<ConnectedDatasetView> grid;
  private final Button connectButton;

  private Context context;

  public ConnectedResourcesComponent(AssociatedDatasetService associatedDatasetService) {
    this.associatedDatasetService = associatedDatasetService;
    addClassNames("padding-horizontal-07", "padding-vertical-04", "flex-vertical");
    addClassName("datasets-content");

    // ── Action bar (always visible) ──────────────────────────────────
    connectButton = new Button("Connect Datasets", VaadinIcon.PLUS_CIRCLE.create());
    connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectButton.addClickListener(e -> fireConnectDatasetsClick());

    var syncAllButton = new Button("Sync All", VaadinIcon.REFRESH.create());
    syncAllButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    syncAllButton.setEnabled(false); // Reserved for FEAT-DATSET-04

    var actionBar = new ActionBar();
    actionBar.addButton(connectButton);
    actionBar.addButton(syncAllButton);

    var header = new SectionHeader(
        new SectionTitle("Connected Resources"),
        actionBar,
        new SectionNote(
            "Datasets connected from InvenioRDM repositories. High-priority "
                + "properties are shown below. Expand a row for more information.")
    );
    header.enableControls();

    section = new Section.SectionBuilder()
        .withHeader(header)
        .build();

    grid = buildGrid();

    // Initial render (empty): the content is populated on refresh() after
    // the context has been set.
    add(section);
  }

  // ── Public API ──────────────────────────────────────────────────────

  /** Sets the project context and renders the content area. */
  public void setContext(Context context) {
    this.context = context;
    refresh();
  }

  /** Reloads connected datasets and re-renders the content area. */
  public void refresh() {
    if (context == null || context.projectId().isEmpty()) {
      return;
    }

    SectionContent content = section.content();
    content.removeAll();

    List<ConnectedDatasetView> datasets = associatedDatasetService.listConnectedDatasetViews(
        context.projectId().orElseThrow());

    if (datasets.isEmpty()) {
      content.add(buildEmptyState());
    } else {
      content.add(grid);
      grid.setItems(datasets);
    }
  }

  /**
   * Registers a listener invoked whenever the "Connect Datasets" button
   * is clicked (from either the action bar or the empty-state CTA).
   */
  public Registration addConnectDatasetsClickListener(
      ComponentEventListener<ConnectDatasetsClickEvent> listener) {
    return addListener(ConnectDatasetsClickEvent.class, listener);
  }

  // ── Empty state ─────────────────────────────────────────────────────

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

    var heading = new H3("No datasets connected");
    heading.getStyle().set("margin", "0 0 var(--lumo-space-s) 0");
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
    cta.addClickListener(e -> fireConnectDatasetsClick());
    wrapper.add(cta);

    return wrapper;
  }

  // ── Grid ────────────────────────────────────────────────────────────

  private Grid<ConnectedDatasetView> buildGrid() {
    var g = new Grid<ConnectedDatasetView>();
    g.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
    g.setSelectionMode(Grid.SelectionMode.NONE);
    g.setWidthFull();

    // Expandable detail row for medium-priority fields
    g.setItemDetailsRenderer(
        new com.vaadin.flow.data.renderer.ComponentRenderer<>(this::buildDetailPanel));

    // Title (high-priority)
    g.addComponentColumn(this::buildTitleCell)
        .setHeader("Title").setFlexGrow(3).setKey("title");

    // PID / DOI
    g.addComponentColumn(this::buildPidCell)
        .setHeader("PID / DOI").setAutoWidth(true).setFlexGrow(1).setKey("pid");

    // Access status (coarse)
    g.addComponentColumn(this::buildAccessStatusCell)
        .setHeader("Access").setAutoWidth(true).setKey("access");

    // Version
    g.addColumn(view -> view.version() != null ? view.version() : "—")
        .setHeader("Version").setAutoWidth(true).setKey("version");

    // Access link
    g.addComponentColumn(this::buildAccessLinkCell)
        .setHeader("Access Link").setAutoWidth(true).setKey("accessLink");

    // Publication date
    g.addComponentColumn(this::buildPublicationDateCell)
        .setHeader("Published").setAutoWidth(true).setKey("publicationDate");

    return g;
  }

  private Component buildTitleCell(ConnectedDatasetView view) {
    var title = new Span(view.title());
    title.addClassName("normal-body-text");
    title.getStyle().set("font-weight", "500");
    return title;
  }

  private Component buildPidCell(ConnectedDatasetView view) {
    String pid = view.pid();
    if (pid == null || pid.isBlank()) {
      return new Span("—");
    }
    String href = pid.startsWith("http") ? pid : "https://doi.org/" + pid;
    var link = new Anchor(href, pid);
    link.setTarget(AnchorTarget.BLANK);
    link.addClassName("extra-small-body-text");
    return link;
  }

  private Component buildAccessStatusCell(ConnectedDatasetView view) {
    var wrapper = new Div();
    if (view.accessLevel() == AccessLevel.PUBLIC) {
      var badge = new Tag("Public");
      badge.setTagColor(TagColor.SUCCESS);
      wrapper.add(badge);
    } else {
      var badge = new Tag("Restricted");
      badge.setTagColor(TagColor.WARNING);
      wrapper.add(badge);
      String accessDetail = view.accessDetail();
      if (accessDetail != null) {
        var detail = new Span(accessDetail);
        detail.addClassName("extra-small-body-text");
        detail.addClassName("color-secondary");
        wrapper.add(detail);
      }
    }
    return wrapper;
  }

  private Component buildAccessLinkCell(ConnectedDatasetView view) {
    String link = view.accessLink();
    if (link == null || link.isBlank()) {
      return new Span("—");
    }
    var anchor = new Anchor(link, "Open ↗");
    anchor.setTarget(AnchorTarget.BLANK);
    anchor.addClassName("extra-small-body-text");
    return anchor;
  }

  private Component buildPublicationDateCell(ConnectedDatasetView view) {
    LocalDate date = view.publicationDate();
    if (date == null) {
      return new Span("—");
    }
    return new Span(date.format(DATE_FMT));
  }

  private Component buildDetailPanel(ConnectedDatasetView view) {
    var panel = new Div();
    panel.addClassNames("flex-vertical", "gap-03");
    panel.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)");
    panel.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
    panel.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

    var heading = new Span("Additional Information");
    heading.getStyle().set("font-weight", "600");
    heading.addClassName("normal-body-text");
    panel.add(heading);

    var body = new Div();
    body.addClassNames("flex-vertical", "gap-02");

    // Connected By — resolved display name, never a raw UUID
    addDetailRow(body, "Connected By", view.connectedByDisplayName());

    // Connected On
    addDetailRow(body, "Connected On",
        view.connectedOn() != null
            ? view.connectedOn().toLocalDate().format(DATE_FMT)
            : "—");

    // Source-specific detail rows
    addDetailRow(body, "Resource Provider", view.resourceProvider());
    addDetailRow(body, "Creator", view.creatorsDisplay());
    addDetailRow(body, "Resource Type",
        view.resourceType() != null ? view.resourceType() : "—");
    addDetailRow(body, "Community",
        view.community() != null ? view.community() : "—");

    // Linked Experiment — clickable name opening the experiment view
    // in a new tab, or "—" when no experiment is linked.
    if (view.experimentId() != null && context.projectId().isPresent()) {
        var href = AppRoutes.ProjectRoutes.EXPERIMENT.formatted(
            context.projectId().get().value(), view.experimentId());
        var anchor = new Anchor(href, view.experimentName());
        anchor.setTarget(AnchorTarget.BLANK);
        anchor.addClassName("normal-body-text");
        anchor.getStyle().set("color", "var(--lumo-primary-text-color)");
        addDetailRow(body, "Linked Experiment", anchor);
    } else {
        addDetailRow(body, "Linked Experiment", "—");
    }

    panel.add(body);
    return panel;
  }

  private void addDetailRow(Div container, String label, String value) {
    addDetailRow(container, label, new Span(value != null ? value : "—"));
  }

  private void addDetailRow(Div container, String label, Component valueComponent) {
    var row = new Div();
    row.addClassNames("flex-horizontal", "gap-02");
    row.getStyle().set("align-items", "baseline");
    var labelSpan = new Span(label + ":");
    labelSpan.addClassName("extra-small-body-text");
    labelSpan.getStyle().set("font-weight", "600");
    labelSpan.getStyle().set("min-width", "160px");
    labelSpan.addClassName("color-secondary");
    valueComponent.addClassName("normal-body-text");
    row.add(labelSpan, valueComponent);
    container.add(row);
  }

  // ── Event wiring ────────────────────────────────────────────────────

  private void fireConnectDatasetsClick() {
    fireEvent(new ConnectDatasetsClickEvent(this));
  }

  /**
   * Custom event fired when either the action-bar "Connect Datasets"
   * button or the empty-state CTA is clicked. The parent view is
   * expected to handle the event by opening the connect-sidebar.
   */
  public static class ConnectDatasetsClickEvent
      extends com.vaadin.flow.component.ComponentEvent<ConnectedResourcesComponent> {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConnectDatasetsClickEvent(ConnectedResourcesComponent source) {
      super(source, false);
    }
  }
}
