package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.OrderedList;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.projects.project.datasets.ConnectedDatasetsMain;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMain;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * "Where the project stands" — the prototype's progress band.
 *
 * <p>Structure (top to bottom): the band title; the attention block (only when something is
 * outstanding, and outside the card); then the progress card with a headline sentence, an ordered
 * rail of three stages ({@code Experiment created → Samples registered → Dataset}) and the
 * dataset-standing block.</p>
 *
 * <p>This is a genuine sequence, so the rail numbers its steps and marks done / attention with
 * glyphs instead of ordinals.</p>
 *
 * @since 1.19.0
 */
public class ProjectProgressSection extends Div {

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  private final transient ProjectQueryService projectQueryService;
  private final transient ProjectInformationService projectInformationService;
  private final Div content = new Div();
  private String projectId;

  public ProjectProgressSection(ProjectQueryService projectQueryService,
      ProjectInformationService projectInformationService) {
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
    this.projectInformationService = requireNonNull(projectInformationService,
        "projectInformationService must not be null");
    addClassName("project-progress-section");
    content.addClassName("project-progress-content");
    add(content);
  }

  public void setProjectId(String projectId) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    reload();
  }

  public Registration addProgressLoadedListener(
      ComponentEventListener<ProgressLoadedEvent> listener) {
    return addListener(ProgressLoadedEvent.class, listener);
  }

  private void reload() {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    content.removeAll();
    content.add(heading());
    content.add(message("Loading project status …"));
    ProjectOverview overview = projectInformationService.findOverview(ProjectId.parse(projectId))
        .orElse(null);
    ReactiveUiBridge.onUi(ui,
        projectQueryService.getExperimentProgress(projectId).collectList(),
        progress -> render(progress, overview),
        error -> {
          content.removeAll();
          content.add(heading());
          content.add(message("Project status could not be loaded."));
        });
  }

  private void render(List<ExperimentProgress> progress, ProjectOverview overview) {
    content.removeAll();
    content.add(heading());
    fireEvent(new ProgressLoadedEvent(this, false, progress));

    List<ExperimentProgress> attention = progress.stream()
        .filter(entry -> entry.measurementsWithoutPrimaryData() > 0)
        .toList();
    if (!attention.isEmpty()) {
      content.add(attentionBlock(attention));
    }

    Div card = new Div();
    card.addClassName("project-progress-card");
    card.add(headline(progress));
    card.add(rail(progress));
    card.add(datasetsStanding(progress, overview));
    content.add(card);
  }

  private Span heading() {
    Span heading = new Span("Where the project stands");
    heading.addClassName("project-progress-heading");
    return heading;
  }

  // ── Attention block (outside the card) ──────────────────────────────────

  private Div attentionBlock(List<ExperimentProgress> attention) {
    Div block = new Div();
    block.addClassName("project-progress-attention");
    Span title = new Span("Needs attention");
    title.addClassName("project-progress-attention-title");
    block.add(title);
    attention.forEach(entry -> block.add(attentionRow(entry)));
    return block;
  }

  private Div attentionRow(ExperimentProgress entry) {
    Div row = new Div();
    row.addClassName("project-progress-attention-row");
    Span context = new Span(entry.experimentName());
    context.addClassName("project-progress-attention-context");
    Span text = new Span(entry.measurementsWithoutPrimaryData() + " of " + entry.measurementCount()
        + " measurements have no primary data");
    text.addClassName("project-progress-attention-text");
    Span action = new Span("Open measurements");
    action.addClassName("project-progress-attention-action");
    row.add(context, separator(), text, separator(), action);
    activateOnClickAndKeys(row, () -> navigate(MeasurementMain.class, entry.experimentId()));
    return row;
  }

  // ── Headline ────────────────────────────────────────────────────────────

  private Div headline(List<ExperimentProgress> progress) {
    Div intro = new Div();
    intro.addClassName("project-progress-headline");
    if (progress.isEmpty()) {
      intro.add(new Span("Nothing has been registered yet. "), strong("Start with one experiment"));
    } else {
      long withSamples = progress.stream().filter(entry -> entry.sampleCount() > 0).count();
      intro.add(strong(withSamples + " of " + progress.size()
              + (progress.size() == 1 ? " experiment" : " experiments")),
          new Span(" have registered samples"));
    }
    return intro;
  }

  // ── Rail of three stages ────────────────────────────────────────────────

  private OrderedList rail(List<ExperimentProgress> progress) {
    int samples = progress.stream().mapToInt(ExperimentProgress::sampleCount).sum();
    int withData = progress.stream().mapToInt(ExperimentProgress::measurementsWithPrimaryData).sum();
    int missing = progress.stream().mapToInt(ExperimentProgress::measurementsWithoutPrimaryData).sum();

    OrderedList rail = new OrderedList();
    rail.addClassName("project-progress-rail");
    rail.add(step(1, "Experiment created", progress.size(), "experiments", State.DONE));
    rail.add(step(2, "Samples registered", samples, "samples",
        samples > 0 ? State.DONE : State.TODO));
    rail.add(step(3, "Dataset", withData, "measurements with data",
        missing > 0 ? State.ATTENTION : State.DONE));
    return rail;
  }

  private ListItem step(int ordinal, String label, int count, String unit, State state) {
    ListItem item = new ListItem();
    item.addClassName("project-progress-step");
    item.addClassName("project-progress-step--" + state.css());

    Span marker = new Span();
    marker.addClassName("project-progress-marker");
    switch (state) {
      case DONE -> marker.add(icon(VaadinIcon.CHECK));
      case ATTENTION -> marker.add(icon(VaadinIcon.WARNING));
      case TODO -> marker.setText(Integer.toString(ordinal));
    }
    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-progress-step-label");
    Span countSpan = new Span(count + " " + unit);
    countSpan.addClassName("project-progress-step-count");
    item.add(marker, labelSpan, countSpan);
    return item;
  }

  // ── Dataset standing ────────────────────────────────────────────────────

  private Div datasetsStanding(List<ExperimentProgress> progress, ProjectOverview overview) {
    Div wrap = new Div();
    wrap.addClassName("project-progress-datasets");

    Div head = new Div();
    head.addClassName("project-progress-datasets-head");
    Span label = new Span("Datasets");
    label.addClassName("project-progress-datasets-label");
    Span open = new Span("Open datasets");
    open.addClassName("project-progress-datasets-link");
    activateOnClickAndKeys(open, () -> getUI().ifPresent(
        ui -> ui.navigate(ConnectedDatasetsMain.class, projectParameters())));
    head.add(label, open);

    Div grid = new Div();
    grid.addClassName("project-progress-datasets-grid");
    grid.add(primaryDataCard(progress));
    grid.add(connectedAssetsCard(overview));
    wrap.add(head, grid);
    return wrap;
  }

  private static Div primaryDataCard(List<ExperimentProgress> progress) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    card.add(cardTitle("Primary data — for analysis"));
    List<String> types = measuredTypes(progress);
    card.add(cardFact(types.isEmpty() ? "No primary data yet" : String.join(" · ", types)));
    long files = progress.stream().mapToLong(ExperimentProgress::primaryDataFiles).sum();
    long bytes = progress.stream().mapToLong(ExperimentProgress::primaryDataBytes).sum();
    card.add(cardFact(humanReadableGigabytes(bytes) + " GB in " + files + " files"));
    return card;
  }

  private Div connectedAssetsCard(ProjectOverview overview) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    int count = overview == null ? 0 : overview.connectedDatasetCount();
    card.add(cardTitle("Connected assets — " + count
        + (count == 1 ? ", hosted elsewhere" : ", hosted elsewhere")));
    card.add(cardFact(count == 0 ? "None linked yet" : count + " linked"));
    return card;
  }

  private static List<String> measuredTypes(List<ExperimentProgress> progress) {
    List<String> types = new ArrayList<>();
    if (progress.stream().anyMatch(entry -> entry.ngsMeasurementCount() > 0)) {
      types.add("Genomics");
    }
    if (progress.stream().anyMatch(entry -> entry.pxpMeasurementCount() > 0)) {
      types.add("Proteomics");
    }
    if (progress.stream().anyMatch(entry -> entry.ipMeasurementCount() > 0)) {
      types.add("Immunopeptidomics");
    }
    return types;
  }

  private static Span cardTitle(String text) {
    Span title = new Span(text);
    title.addClassName("project-progress-datasets-card-title");
    return title;
  }

  private static Span cardFact(String text) {
    Span fact = new Span(text);
    fact.addClassName("project-progress-datasets-card-fact");
    return fact;
  }

  private static String humanReadableGigabytes(long bytes) {
    return "%.1f".formatted(bytes / 1_000_000_000.0);
  }

  // ── Helpers ─────────────────────────────────────────────────────────────

  private void navigate(Class<? extends com.vaadin.flow.component.Component> target,
      String experimentId) {
    RouteParameters parameters = new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId),
        new RouteParam(EXPERIMENT_ID_PARAMETER, experimentId));
    getUI().ifPresent(ui -> ui.navigate(target, parameters));
  }

  private RouteParameters projectParameters() {
    return new RouteParameters(new RouteParam(PROJECT_ID_PARAMETER, projectId));
  }

  private static void activateOnClickAndKeys(com.vaadin.flow.component.Component element,
      Runnable action) {
    element.getElement().setAttribute("role", "link");
    element.getElement().setAttribute("tabindex", "0");
    element.getElement().addEventListener("click", event -> action.run());
    element.getElement().addEventListener("keydown", event -> {
      String key = event.getEventData().path("event.key").asText("");
      if ("Enter".equals(key) || " ".equals(key)) {
        action.run();
      }
    }).addEventData("event.key");
  }

  private static Icon icon(VaadinIcon vaadinIcon) {
    Icon icon = vaadinIcon.create();
    icon.addClassName("project-progress-glyph");
    return icon;
  }

  private static Span strong(String text) {
    Span span = new Span(text);
    span.addClassName("project-progress-strong");
    return span;
  }

  private static Span separator() {
    Span separator = new Span("·");
    separator.addClassName("project-progress-separator");
    return separator;
  }

  private static Span message(String text) {
    Span message = new Span(text);
    message.addClassName("project-progress-message");
    return message;
  }

  private enum State {
    DONE("done"), ATTENTION("attention"), TODO("todo");

    private final String css;

    State(String css) {
      this.css = css;
    }

    String css() {
      return css;
    }
  }

  /** Fired after the project progress has been loaded and rendered. */
  public static class ProgressLoadedEvent extends ComponentEvent<ProjectProgressSection> {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<ExperimentProgress> progress;

    public ProgressLoadedEvent(ProjectProgressSection source, boolean fromClient,
        List<ExperimentProgress> progress) {
      super(source, fromClient);
      this.progress = List.copyOf(progress);
    }

    public List<ExperimentProgress> progress() {
      return progress;
    }
  }
}
