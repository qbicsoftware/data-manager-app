package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
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
import java.util.List;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.datasets.ConnectedDatasetsMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * "Where the project stands" — the prototype's progress band.
 *
 * <p>Structure (top to bottom): the band title; the attention card (only when something is
 * outstanding); then the progress card with a headline sentence, a rail of three stages
 * ({@code Experiment created -> Samples registered -> Dataset}) on a connector line, and the
 * dataset standing (primary data and connected assets).</p>
 *
 * <p>This is a genuine sequence, so the rail numbers its steps and marks done / attention with
 * glyphs instead of ordinals. Each step routes to its surface.</p>
 *
 * @since 1.19.0
 */
public class ProjectProgressSection extends Div {

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  private final transient ProjectQueryService projectQueryService;
  private final transient ProjectInformationService projectInformationService;
  private final transient AssociatedDatasetService associatedDatasetService;
  private final Div content = new Div();
  private String projectId;

  public ProjectProgressSection(ProjectQueryService projectQueryService,
      ProjectInformationService projectInformationService,
      AssociatedDatasetService associatedDatasetService) {
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
    this.projectInformationService = requireNonNull(projectInformationService,
        "projectInformationService must not be null");
    this.associatedDatasetService = requireNonNull(associatedDatasetService,
        "associatedDatasetService must not be null");
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
    ProjectId id = ProjectId.parse(projectId);
    ProjectOverview overview = projectInformationService.findOverview(id).orElse(null);
    List<ConnectedDatasetView> connected = associatedDatasetService.listConnectedDatasetViews(id);
    ReactiveUiBridge.onUi(ui,
        projectQueryService.getExperimentProgress(projectId).collectList(),
        progress -> render(progress, overview, connected),
        error -> {
          content.removeAll();
          content.add(heading());
          content.add(message("Project status could not be loaded."));
        });
  }

  private void render(List<ExperimentProgress> progress, ProjectOverview overview,
      List<ConnectedDatasetView> connected) {
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
    card.add(datasetsStanding(progress, overview, connected));
    content.add(card);
  }

  private Span heading() {
    Span heading = new Span("Where the project stands");
    heading.addClassName("project-progress-heading");
    return heading;
  }

  // ── Attention card ─────────────────────────────────────────────────────

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
    row.add(icon(VaadinIcon.WARNING));
    Span context = new Span(entry.experimentName());
    context.addClassName("project-progress-attention-context");
    Span text = new Span(entry.measurementsWithoutPrimaryData() + " of " + entry.measurementCount()
        + " measurements have no primary data attached");
    text.addClassName("project-progress-attention-text");
    Span action = new Span("Open dataset");
    action.addClassName("project-progress-attention-action");
    row.add(context, text, action);
    Utility.activateOnClickAndKeys(row, "link",
        () -> navigate(MeasurementMain.class, entry.experimentId()));
    return row;
  }

  // ── Headline ───────────────────────────────────────────────────────────

  private Div headline(List<ExperimentProgress> progress) {
    Div intro = new Div();
    intro.addClassName("project-progress-headline");
    if (progress.isEmpty()) {
      intro.add(new Span("Nothing has been registered yet. "), strong("Start with one experiment"));
    } else {
      long withSamples = progress.stream().filter(entry -> entry.sampleCount() > 0).count();
      intro.add(strong(withSamples + " of " + progress.size()
              + (progress.size() == 1 ? " experiment" : " experiments")),
          new Span(progress.size() == 1 ? " has registered samples" : " have registered samples"));
    }
    return intro;
  }

  // ── Rail of three stages ───────────────────────────────────────────────

  private OrderedList rail(List<ExperimentProgress> progress) {
    int samples = progress.stream().mapToInt(ExperimentProgress::sampleCount).sum();
    int measurements = progress.stream().mapToInt(ExperimentProgress::measurementCount).sum();
    int withData = progress.stream().mapToInt(ExperimentProgress::measurementsWithPrimaryData).sum();
    int missing = progress.stream().mapToInt(ExperimentProgress::measurementsWithoutPrimaryData).sum();
    String firstExperiment = progress.isEmpty() ? null : progress.getFirst().experimentId();

    OrderedList rail = new OrderedList();
    rail.addClassName("project-progress-rail");
    rail.add(step(1, "Experiment created", pluralise(progress.size(), "experiment", "experiments"),
        State.DONE, ExperimentInformationMain.class, firstExperiment));
    rail.add(step(2, "Samples registered", pluralise(samples, "sample", "samples"),
        samples > 0 ? State.DONE : State.TODO, SampleInformationMain.class, firstExperiment));
    // "8 of 272", not "8". The other steps state how much there is; this one also has to state
    // how much is missing, and a count without its denominator cannot do that.
    rail.add(step(3, "Dataset",
        "%d of %s".formatted(withData,
            pluralise(measurements, "measurement", "measurements")),
        missing > 0 ? State.ATTENTION : State.DONE, MeasurementMain.class, firstExperiment));
    return rail;
  }

  private static String pluralise(int count, String singular, String plural) {
    return count + " " + (count == 1 ? singular : plural);
  }

  private ListItem step(int ordinal, String label, String count,
      State state, Class<? extends Component> target, String experimentId) {
    ListItem item = new ListItem();
    item.addClassName("project-progress-step");
    item.addClassName("project-progress-step--" + state.css());

    Div head = new Div();
    head.addClassName("project-progress-step-head");
    Span marker = new Span();
    marker.addClassName("project-progress-marker");
    switch (state) {
      case DONE -> marker.add(icon(VaadinIcon.CHECK));
      case ATTENTION -> marker.add(icon(VaadinIcon.WARNING));
      case TODO -> marker.setText(Integer.toString(ordinal));
    }
    Span connector = new Span();
    connector.addClassName("project-progress-connector");
    head.add(marker, connector);

    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-progress-step-label");
    Span countSpan = new Span(count);
    countSpan.addClassName("project-progress-step-count");
    item.add(head, labelSpan, countSpan);
    if (experimentId != null) {
      item.addClassName("project-progress-step--link");
      Utility.activateOnClickAndKeys(item, "link", () -> navigate(target, experimentId));
    }
    return item;
  }

  // ── Dataset standing ───────────────────────────────────────────────────

  private Div datasetsStanding(List<ExperimentProgress> progress, ProjectOverview overview,
      List<ConnectedDatasetView> connected) {
    Div wrap = new Div();
    wrap.addClassName("project-progress-datasets");

    Div head = new Div();
    head.addClassName("project-progress-datasets-head");
    Span label = new Span("Datasets");
    label.addClassName("project-progress-datasets-label");
    Span open = new Span("Open datasets");
    open.addClassName("project-progress-datasets-link");
    Utility.activateOnClickAndKeys(open, "link", () -> getUI().ifPresent(
        ui -> ui.navigate(ConnectedDatasetsMain.class, projectParameters())));
    head.add(label, open);

    Div grid = new Div();
    grid.addClassName("project-progress-datasets-grid");
    grid.add(primaryDataCard(progress));
    grid.add(connectedAssetsCard(connected));
    wrap.add(head, grid);
    return wrap;
  }

  /**
   * Primary data, as analysis sees it: how completely it covers the registered measurements, and
   * how much of it there is per assay type.
   *
   * <p>Coverage leads because it is the part a reader can act on -- "8 of 272" is a to-do list,
   * whereas a byte total is trivia. Volume is then split per assay type rather than summed into
   * one number, because a project total hides whether the mass sits in one modality or is spread
   * across all three. The file count that used to sit here is gone: it is not comparable across
   * assay types, it cannot be acted on, and pairing it with a byte total only invites the reader
   * to divide the two (see the migration's trap 4c).</p>
   */
  private static Div primaryDataCard(List<ExperimentProgress> progress) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    card.add(cardTitle("Primary data — for analysis"));
    card.add(cardFact(coverage(progress)));
    card.add(cardFact(volumeByAssayType(progress)));
    return card;
  }

  private static String coverage(List<ExperimentProgress> progress) {
    int measurements = progress.stream().mapToInt(ExperimentProgress::measurementCount).sum();
    if (measurements == 0) {
      return "No measurements registered yet";
    }
    int withData = progress.stream()
        .mapToInt(ExperimentProgress::measurementsWithPrimaryData).sum();
    return "%d of %s %s primary data".formatted(withData,
        pluralise(measurements, "measurement", "measurements"),
        withData == 1 ? "has" : "have");
  }

  private record AssayType(String label, int measurementsWithData, long bytes) {

  }

  /** The assay types that actually carry primary data, in the order the rail names them. */
  private static List<AssayType> assayTypesWithPrimaryData(List<ExperimentProgress> progress) {
    return List.of(
            new AssayType("Genomics",
                progress.stream()
                    .mapToInt(ExperimentProgress::ngsMeasurementsWithPrimaryData).sum(),
                progress.stream().mapToLong(ExperimentProgress::ngsPrimaryDataBytes).sum()),
            new AssayType("Proteomics",
                progress.stream()
                    .mapToInt(ExperimentProgress::pxpMeasurementsWithPrimaryData).sum(),
                progress.stream().mapToLong(ExperimentProgress::pxpPrimaryDataBytes).sum()),
            new AssayType("Immunopeptidomics",
                progress.stream()
                    .mapToInt(ExperimentProgress::ipMeasurementsWithPrimaryData).sum(),
                progress.stream().mapToLong(ExperimentProgress::ipPrimaryDataBytes).sum()))
        .stream().filter(type -> type.measurementsWithData() > 0).toList();
  }

  /**
   * The volume, per assay type.
   *
   * <p>A type whose size was never recorded shows "—", not "0 B": those are different claims,
   * and a real project reports 0 bytes for every measurement it holds. When no size was recorded
   * anywhere, the types are still named but the gap is stated once, since a row of dashes is
   * noise rather than information.</p>
   */
  private static String volumeByAssayType(List<ExperimentProgress> progress) {
    List<AssayType> present = assayTypesWithPrimaryData(progress);
    if (present.isEmpty()) {
      return "No primary data attached yet";
    }
    if (present.stream().allMatch(type -> type.bytes() == 0)) {
      return String.join(" · ", present.stream().map(AssayType::label).toList())
          + " — size not recorded";
    }
    return String.join(" · ", present.stream()
        .map(type -> type.label() + " " + humanReadableSize(type.bytes()))
        .toList());
  }

  /**
   * Size in the unit that keeps the number readable. Decimal units, matching the rest of the app.
   *
   * <p>Zero means "not recorded" at this point in the flow; see {@link #volumeByAssayType}.</p>
   */
  private static String humanReadableSize(long bytes) {
    if (bytes <= 0) {
      return "—";
    }
    if (bytes < 1_000) {
      return bytes + " B";
    }
    if (bytes < 1_000_000) {
      return "%.1f kB".formatted(bytes / 1_000.0);
    }
    if (bytes < 1_000_000_000) {
      return "%.1f MB".formatted(bytes / 1_000_000.0);
    }
    return "%.1f GB".formatted(bytes / 1_000_000_000.0);
  }

  private static Div connectedAssetsCard(List<ConnectedDatasetView> connected) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    int count = connected.size();
    card.add(cardTitle("Connected assets — " + count + ", hosted elsewhere"));
    if (connected.isEmpty()) {
      card.add(cardFact("None linked yet"));
      return card;
    }
    // Titles from external repositories run to a full sentence. Clamped to one line with the full
    // text in the tooltip, so three entries stay scannable instead of collapsing into prose.
    connected.stream()
        .limit(3)
        .forEach(asset -> card.add(truncatedCardFact(
            asset.title() + " · " + asset.resourceProvider())));
    return card;
  }

  private static Span truncatedCardFact(String text) {
    Span fact = cardFact(text);
    fact.addClassName("project-progress-datasets-card-fact--truncated");
    fact.getElement().setAttribute("title", text);
    return fact;
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

  // ── Helpers ─────────────────────────────────────────────────────────────

  private void navigate(Class<? extends Component> target, String experimentId) {
    RouteParameters parameters = new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId),
        new RouteParam(EXPERIMENT_ID_PARAMETER, experimentId));
    getUI().ifPresent(ui -> ui.navigate(target, parameters));
  }

  private RouteParameters projectParameters() {
    return new RouteParameters(new RouteParam(PROJECT_ID_PARAMETER, projectId));
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
