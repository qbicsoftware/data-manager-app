package life.qbic.datamanager.views.projects.project.datasets;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.DataSetTagFactory;
import life.qbic.datamanager.views.general.DataSetTagFactory.TagType;
import life.qbic.datamanager.views.general.section.ActionBar;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.SectionContent;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionNote;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;

/**
 * <b>Connected Datasets Component</b>
 *
 * <p>Renders the "Datasets" section within the associated
 * datasets view. Contains:</p>
 * <ul>
 *   <li>an action bar with a primary "Connect Datasets" button;</li>
 *   <li>a <b>rich row card list</b> of connected datasets, with high-priority
 * *       properties (title, PID, version, access link, published date) in the
 *       primary tier, the resource type as a header-row badge for fast
 *       scanning, and medium-priority properties (connected by, creator,
 *       community, linked experiment) always visible below
 *       a subtle tier separator; the provenance line "connected on …"
 *       and attribution are rendered as a single italic footer note.</li>
 *   <li>an empty-state guidance panel when no datasets are connected yet,</li>
 *   <li>a CTA button that opens the connect-sidebar to search and connect
 *       datasets from InvenioRDM repositories.</li>
 * </ul>
 *
 * <p>Cards consume {@link ConnectedDatasetView} DTOs from the application
 * layer (never domain entities directly). User and experiment display names
 * are already resolved by the service — this component renders them as-is.</p>
 *
 * <p>Per FEAT-DATSET-01 this component <b>does not</b> render per-row actions
 * (Sync, Remove) — those belong to later stories (FEAT-DATSET-04 and
 * beyond).</p>
 *
 * @since 1.12.0
 */
public class ConnectedResourcesComponent extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

  private static final String SECONDARY_COLOR = "var(--lumo-secondary-text-color)";

  private final AssociatedDatasetService associatedDatasetService;
  private final Section section;
  private final Div cardsContainer;
  private final Button connectButton;

  private Context context;

  private boolean writeAllowed = false;

  public ConnectedResourcesComponent(AssociatedDatasetService associatedDatasetService) {
    this.associatedDatasetService = associatedDatasetService;
    addClassNames("padding-horizontal-07", "padding-vertical-04", "flex-vertical");
    addClassName("datasets-content");

    // ── Action bar (always visible) ──────────────────────────────────
    connectButton = new Button("Connect Datasets", VaadinIcon.PLUS_CIRCLE.create());
    connectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    connectButton.addClickListener(e -> fireConnectDatasetsClick());

    var actionBar = new ActionBar(connectButton);

    var note = new SectionNote(
        "External datasets connected to this project and its experiments. "
            + "Click any DOI or access link to open the record on the source.");
    var header = new SectionHeader(
        new SectionTitle("All datasets"),
        actionBar,
        note
    );
    header.enableControls();

    section = new Section.SectionBuilder()
        .withHeader(header)
        .build();

    // ── Card container ───────────────────────────────────────────────
    cardsContainer = new Div();
    cardsContainer.addClassNames("flex-vertical");
    cardsContainer.getStyle().set("gap", "var(--lumo-space-m)");

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

  /** Controls whether the "Remove" button is rendered on each card. */
  public void setWriteAllowed(boolean writeAllowed) {
    this.writeAllowed = writeAllowed;
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

    cardsContainer.removeAll();
    if (datasets.isEmpty()) {
      content.add(buildEmptyState());
    } else {
      for (ConnectedDatasetView view : datasets) {
        cardsContainer.add(buildCard(view));
      }
      content.add(cardsContainer);
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

  /**
   * Registers a listener invoked when the "Remove" button on a dataset
   * card is clicked. Listener receives the aggregate ID of the clicked
   * dataset. Only fires when {@link #setWriteAllowed(boolean)} is
   * {@code true}.
   *
   * @since 1.12.0
   */
  public Registration addRemoveDatasetClickListener(
      ComponentEventListener<RemoveDatasetClickEvent> listener) {
    return addListener(RemoveDatasetClickEvent.class, listener);
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

  // ─ Card builder ────────────────────────────────────────────────────

  private Component buildCard(ConnectedDatasetView view) {
    var card = new Div();
    card.addClassNames("rounded-02");
    card.getStyle().set("display", "flex");
    card.getStyle().set("flex-direction", "column");
    card.getStyle().set("padding", "0");
    card.getStyle().set("overflow", "hidden");
    card.getStyle().set("background-color", "var(--lumo-base-color)");
    // Explicit border — .border class is too subtle for card listings;
    // use a slightly darker contrast token so individual cards are
    // visually distinguishable even when stacked densely.
    card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");

    // ═══ HEADER ROW: Provider · Access · Published ═══
    var headerRow = new Div();
    headerRow.addClassNames("flex-horizontal", "items-center", "gap-03");
    headerRow.getStyle().set("padding",
        "var(--lumo-space-m) var(--lumo-space-m) var(--lumo-space-s) var(--lumo-space-m)");
    headerRow.getStyle().set("flex-wrap", "wrap");

    // Provider tag — styled via centralized factory so this view and the
    // connect-datasets sidebar share the same color scheme.
    String provider = view.resourceProvider();
    if (provider != null && !provider.isBlank()) {
      headerRow.add(DataSetTagFactory.create(TagType.PROVIDER, provider));
    }

    // Access badge — styled via centralized factory
    headerRow.add(DataSetTagFactory.create(
        TagType.ACCESS_TYPE, view.isPublic()));

    // Resource type badge — elevated to the header row for fast scanning.
    // Rendered as a neutral CONTRAST tag so it does not compete with the
    // provider (PRIMARY) or access-level (SUCCESS/WARNING) badges.
    String resourceType = view.resourceType();
    if (resourceType != null && !resourceType.isBlank()) {
      headerRow.add(DataSetTagFactory.create(TagType.DATA_SET_TYPE, resourceType));
    }

    // Access-detail note for restricted datasets (inline after badge)
    String accessDetail = view.accessDetail();
    if (accessDetail != null && !accessDetail.isBlank()) {
      var detailNote = new Span(accessDetail);
      detailNote.addClassName("extra-small-body-text");
      detailNote.getStyle().set("color", SECONDARY_COLOR);
      detailNote.getStyle().set("font-style", "italic");
      headerRow.add(detailNote);
    }

    // Spacer — pushes everything after it to the right edge.
    var spacer = new Div();
    spacer.getStyle().set("flex-grow", "1");
    headerRow.add(spacer);

    // Published date (right-aligned, secondary info).
    LocalDate pubDate = view.publicationDate();
    if (pubDate != null) {
      var dateSpan = new Span("Published: " + pubDate.format(DATE_FMT));
      dateSpan.addClassName("extra-small-body-text");
      dateSpan.getStyle().set("color", SECONDARY_COLOR);
      headerRow.add(dateSpan);
    }

    // Remove button (write-access only, right-aligned as a primary action).
    if (writeAllowed) {
      var removeButton = new Button(VaadinIcon.TRASH.create());
      removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE,
          ButtonVariant.LUMO_ERROR);
      removeButton.getElement().setAttribute("title", "Remove dataset connection");
      removeButton.getElement().setAttribute("aria-label", "Remove dataset connection");
      removeButton.addClickListener(e -> fireRemoveDatasetClick(view.id()));
      headerRow.add(removeButton);
    }

    card.add(headerRow);

    // ═══ TITLE BLOCK ═══
    var titleBlock = new Div();
    titleBlock.getStyle().set("padding",
        "0 var(--lumo-space-m) var(--lumo-space-m) var(--lumo-space-m)");
    titleBlock.getStyle().set("min-width", "0");

    var titleSpan = new Span(view.title());
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

    // PID (linked to DOI resolver)
    String pidHref = view.pid().startsWith("http")
        ? view.pid() : "https://doi.org/" + view.pid();
    var pidLink = new Anchor(pidHref, view.pid());
    pidLink.setTarget(AnchorTarget.BLANK);
    pidLink.addClassName("extra-small-body-text");
    metaRow.add(pidLink);

    // Version (hide when remote info is missing — avoid meaningless "—")
    String vNorm = view.version();
    if (vNorm != null && !vNorm.isBlank()) {
      String display = "v" + vNorm.replaceFirst("^v", "");
      var versionSpan = new Span(display);
      versionSpan.addClassName("extra-small-body-text");
      versionSpan.getStyle().set("color", SECONDARY_COLOR);
      addMetaSeparator(metaRow);
      metaRow.add(versionSpan);
    }

    // Access link (hide when missing)
    String accessLink = view.accessLink();
    if (accessLink != null && !accessLink.isBlank()) {
      addMetaSeparator(metaRow);
      var accessAnchor = new Anchor(accessLink, "Access ↗");
      accessAnchor.setTarget(AnchorTarget.BLANK);
      accessAnchor.addClassName("extra-small-body-text");
      accessAnchor.getElement().setAttribute("title", accessLink);
      metaRow.add(accessAnchor);
    }

    card.add(metaRow);

    // ══ MEDIUM-PRIO TIER ═══
    var detailRow = buildDetailTier(view);
    card.add(detailRow);

    return card;
  }

  private Div buildDetailTier(ConnectedDatasetView view) {
    var row = new Div();
    row.addClassNames("flex-horizontal", "items-start");
    row.getStyle().set("padding",
        "var(--lumo-space-m) var(--lumo-space-m)");
    row.getStyle().set("flex-wrap", "wrap");
    row.getStyle().set("gap", "var(--lumo-space-m) var(--lumo-space-xl)");
    row.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
    row.getStyle().set("border-top", "2px solid var(--lumo-contrast-10pct)");

    // Creator — truncate long lists with tooltip showing full names;
    // hide entirely when remote info is missing.
    List<String> creators = view.creators();
    if (creators != null && !creators.isEmpty()) {
        addDetailCellCreator(row, creators);
    }

    // Resource type is shown in the card header row.

    // Community — hide when remote info is missing.
    String community = view.community();
    if (community != null && !community.isBlank()) {
      addDetailCell(row, "Community", community);
    }

    // Linked experiment — clickable link to the experiment view in a new tab;
    // hide entirely when not linked.
    if (view.experimentId() != null) {
        String expName = view.experimentName() != null ? view.experimentName() : view.experimentId();
        Component experimentComponent;
        if (context.projectId().isPresent()) {
            var href = AppRoutes.ProjectRoutes.EXPERIMENT.formatted(
                context.projectId().get().value(), view.experimentId());
            var anchor = new Anchor(href, expName);
            anchor.setTarget(AnchorTarget.BLANK);
            anchor.addClassName("extra-small-body-text");
            anchor.getStyle().set("color", "var(--lumo-primary-text-color)");
            experimentComponent = anchor;
        } else {
            experimentComponent = new Span(expName);
            experimentComponent.addClassName("extra-small-body-text");
        }
        addDetailCellComponent(row, "Linked Experiment", experimentComponent);
    }

    // Attribution — fused inline sentence combining who and when.
    // These two fields are semantically the same event (the connection action),
    // so rendering them as two separate label: value cells is redundant and
    // eats horizontal space. A single natural-language line reads faster and
    // lets the previous cells breathe. Verb-first order ("connected …") is
    // chosen so the line reads as a complete attribution phrase rather than a
    // label-value pair.
    String connectedBy = view.connectedByDisplayName();
    LocalDate connectedOn = view.connectedOn() != null
        ? LocalDate.ofInstant(view.connectedOn(), ZoneOffset.UTC)
        : null;
    if ((connectedBy != null && !connectedBy.isBlank()) && connectedOn != null) {
      var attribCell = new Div();
      attribCell.addClassName("extra-small-body-text");
      attribCell.getStyle().set("color", SECONDARY_COLOR);
      attribCell.getStyle().set("font-style", "italic");
      attribCell.setText("connected on " + connectedOn.format(DATE_FMT) + " by " + connectedBy);
      row.add(attribCell);
    } else if (connectedBy != null && !connectedBy.isBlank()) {
      var attribCell = new Div();
      attribCell.addClassName("extra-small-body-text");
      attribCell.getStyle().set("color", SECONDARY_COLOR);
      attribCell.getStyle().set("font-style", "italic");
      attribCell.setText("connected by " + connectedBy);
      row.add(attribCell);
    } else if (connectedOn != null) {
      var attribCell = new Div();
      attribCell.addClassName("extra-small-body-text");
      attribCell.getStyle().set("color", SECONDARY_COLOR);
      attribCell.getStyle().set("font-style", "italic");
      attribCell.setText("connected on " + connectedOn.format(DATE_FMT));
      row.add(attribCell);
    }

    return row;
  }

  private void addDetailCellCreator(Div container, List<String> creators) {
    var cell = new Div();
    cell.addClassNames("flex-horizontal", "gap-02", "items-baseline");

    var labelSpan = new Span("Creator:");
    labelSpan.addClassNames("extra-small-body-text");
    labelSpan.getStyle().set("font-weight", "600");
    labelSpan.getStyle().set("color", SECONDARY_COLOR);
    cell.add(labelSpan);

    // Truncate at 2 creators; show full list in tooltip.
    String display;
    if (creators.size() <= 2) {
        display = String.join(", ", creators);
    } else {
        display = creators.get(0) + ", " + creators.get(1)
            + " …and " + (creators.size() - 2) + " more";
    }

    var valueSpan = new Span(display);
    valueSpan.addClassNames("extra-small-body-text");
    // Always expose the full list via tooltip, regardless of truncation.
    valueSpan.getElement().setAttribute("title", String.join(", ", creators));
    cell.add(valueSpan);
    container.add(cell);
  }

  /**
   * Renders a label-value pair in the detail tier. When {@code value} is
   * {@code null} or blank the cell is omitted entirely — the caller is
   * responsible for only invoking this when there is something meaningful
   * to show.
   */
  private void addDetailCell(Div container, String label, String value) {
    if (value == null) {
      return;
    }
    addDetailCellComponent(container, label, new Span(value));
  }

  private void addDetailCellComponent(Div container, String label, Component valueComponent) {
    var cell = new Div();
    cell.addClassNames("flex-horizontal", "gap-02", "items-baseline");

    var labelSpan = new Span(label + ":");
    labelSpan.addClassName("extra-small-body-text");
    labelSpan.getStyle().set("font-weight", "600");
    labelSpan.getStyle().set("color", SECONDARY_COLOR);

    if (valueComponent instanceof Span s) {
      s.addClassName("extra-small-body-text");
    }
    cell.add(labelSpan, valueComponent);
    container.add(cell);
  }

  private void addMetaSeparator(Div row) {
    var sep = new Span("·");
    sep.addClassName("extra-small-body-text");
    sep.getStyle().set("color", "var(--lumo-contrast-30pct)");
    row.add(sep);
  }

  // ─ Event wiring ──────────────────────────────────────

  private void fireConnectDatasetsClick() {
    fireEvent(new ConnectDatasetsClickEvent(this));
  }

  private void fireRemoveDatasetClick(String datasetId) {
    fireEvent(new RemoveDatasetClickEvent(this, datasetId));
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

  /**
   * Custom event fired when the "Remove" button on a dataset card is
   * clicked. Carries the aggregate ID of the dataset to remove.
   *
   * @since 1.12.0
   */
  public static class RemoveDatasetClickEvent
      extends com.vaadin.flow.component.ComponentEvent<ConnectedResourcesComponent> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String datasetId;

    public RemoveDatasetClickEvent(ConnectedResourcesComponent source, String datasetId) {
      super(source, false);
      this.datasetId = datasetId;
    }

    /** Aggregate ID (UUID string) of the dataset to remove. */
    public String getDatasetId() {
      return datasetId;
    }
  }
}
