package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.datamanager.views.general.CompletenessRing;
import life.qbic.datamanager.views.general.DataSetTagFactory;
import life.qbic.datamanager.views.general.DataSetTagFactory.TagType;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.datasets.ProjectDatasetsMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;
import life.qbic.projectmanagement.application.query.AssayType;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import reactor.core.publisher.Mono;

/**
 * "Where the project stands" — the prototype's progress band.
 *
 * <p>Structure (top to bottom): the band title; the not-complete card (only when something is
 * outstanding); then the progress card with a headline sentence, a rail of three stages
 * ({@code Experiment created -> Samples registered -> Dataset}) on a connector line, and the
 * dataset standing (primary data and connected assets).</p>
 *
 * <p>This is a genuine sequence, so the rail numbers its steps and marks done / not-complete with
 * glyphs instead of ordinals. Each step routes to its surface.</p>
 *
 * <p><b>Label vs. detail.</b> A step is two strings, and the split is what keeps the rail honest:
 * the <em>label</em> names the thing and never changes with state ("Experiment", "Samples",
 * "Dataset"); the <em>detail</em> states the state and is the only part that varies ("Create one to
 * begin", "3 created", "Unlocks once an experiment exists"). A single merged label cannot do this:
 * "Experiment created" is a status occupying the identity slot, so it asserts an outcome whatever
 * the marker says and reads as a false claim on the empty project. Keep the label a noun.</p>
 *
 * @since 1.19.0
 */
public class ProjectProgressSection extends Div {

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  /**
   * Document id of the per-experiment not-complete list, so the identity band can link straight to
   * it.
   *
   * <p>The two live in sibling components and neither knows the other's DOM, so the id is the
   * contract between them. Unique per page, since a page renders one progress band.</p>
   */
  public static final String INCOMPLETE_LIST_ID = "project-incomplete";

  private final transient AssociatedDatasetService associatedDatasetService;
  private final Div content = new Div();
  private String projectId;

  public ProjectProgressSection(AssociatedDatasetService associatedDatasetService) {
    this.associatedDatasetService = requireNonNull(associatedDatasetService,
        "associatedDatasetService must not be null");
    addClassName("project-progress-section");
    content.addClassName("project-progress-content");
    add(content);
  }

  /**
   * Loads the band for {@code projectId} from data the owning page already holds.
   *
   * <p>The {@link ProjectOverview} and the shared {@code progress} stream are supplied by the
   * parent rather than fetched here, so the summary page issues one progress query and one
   * overview read for both of its sections instead of one each. The parent caches the stream, so
   * the two subscribers share a single execution.</p>
   */
  public void setProjectId(String projectId, ProjectOverview overview,
      Mono<List<ExperimentProgress>> progress) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    reload(requireNonNull(overview, "overview must not be null"),
        requireNonNull(progress, "progress must not be null"));
  }

  public Registration addProgressLoadedListener(
      ComponentEventListener<ProgressLoadedEvent> listener) {
    return addListener(ProgressLoadedEvent.class, listener);
  }

  private void reload(ProjectOverview overview, Mono<List<ExperimentProgress>> progress) {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    content.removeAll();
    content.add(heading());
    content.add(message("Loading project status …"));
    ProjectId id = ProjectId.parse(projectId);
    List<ConnectedDatasetView> connected = associatedDatasetService.listConnectedDatasetViews(id);
    ReactiveUiBridge.onUi(ui, progress,
        loaded -> render(loaded, overview, connected),
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

    List<ExperimentProgress> incomplete = progress.stream()
        .filter(entry -> entry.measurementsWithoutPrimaryData() > 0)
        .toList();
    if (!incomplete.isEmpty()) {
      content.add(incompleteBlock(incomplete));
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

  // ── Not-complete card ──────────────────────────────────────────────────

  private Div incompleteBlock(List<ExperimentProgress> incomplete) {
    Div block = new Div();
    block.addClassName("project-progress-incomplete");
    block.setId(INCOMPLETE_LIST_ID);
    // "Not complete yet", not "Needs attention": missing primary data is a normal, time-dependent
    // stage of the process, not a problem, so the card informs rather than alarms.
    Span title = new Span("Not complete yet");
    title.addClassName("project-progress-incomplete-title");
    block.add(title);
    incomplete.forEach(entry -> block.add(incompleteRow(entry)));
    return block;
  }

  private Div incompleteRow(ExperimentProgress entry) {
    Div row = new Div();
    row.addClassName("project-progress-incomplete-row");
    row.add(icon(VaadinIcon.INFO_CIRCLE));
    Span context = new Span(entry.experimentName());
    context.addClassName("project-progress-incomplete-context");
    Span text = new Span(entry.measurementsWithoutPrimaryData() + " of " + entry.measurementCount()
        + " measurements have no primary data attached");
    text.addClassName("project-progress-incomplete-text");
    Span action = new Span("Open dataset");
    action.addClassName("project-progress-incomplete-action");
    row.add(context, text, action);
    Utility.activateOnClickAndKeys(row, "link",
        () -> navigate(MeasurementDatasetMain.class, entry.experimentId()));
    return row;
  }

  // ── Headline ───────────────────────────────────────────────────────────

  private Div headline(List<ExperimentProgress> progress) {
    Div intro = new Div();
    intro.addClassName("project-progress-headline");
    if (progress.isEmpty()) {
      // The sentence states the project's condition; the button states the action. They must not
      // say the same thing: the empty state used to read "Create one experiment to begin" next to a
      // "Create experiment" button, so the headline spent its one line repeating the control beside
      // it. The roadmap (what comes after) is the rail's job, one line below.
      intro.addClassName("project-progress-headline--empty");
      Span text = new Span("No experiment yet.");
      text.addClassName("project-progress-empty-text");
      Button create = new Button("Create experiment", click -> requestCreate());
      create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      create.addClassName("project-progress-empty-action");
      intro.add(text, create);
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
    OrderedList rail = new OrderedList();
    rail.addClassName("project-progress-rail");
    if (progress.isEmpty()) {
      // Nothing registered yet: this is the state the user is most often in and the one that most
      // needs leading, so the pipeline states each step's *prerequisite* rather than a count of
      // zero. Step 1 is ACTIVE (a filled primary marker, clickable) and the two downstream stages
      // are LOCKED. No label here claims an outcome -- see the label/detail note on this class.
      rail.add(step(1, "Experiment", "Create one to begin", State.ACTIVE,
          this::requestCreate));
      rail.add(step(2, "Samples", "Unlocks once an experiment exists", State.LOCKED, null));
      rail.add(step(3, "Dataset", "Unlocks once samples exist", State.LOCKED, null));
      return rail;
    }

    int samples = progress.stream().mapToInt(ExperimentProgress::sampleCount).sum();
    int measurements = progress.stream().mapToInt(ExperimentProgress::measurementCount).sum();
    int withData = progress.stream().mapToInt(ExperimentProgress::measurementsWithPrimaryData).sum();
    int missing = progress.stream().mapToInt(ExperimentProgress::measurementsWithoutPrimaryData).sum();
    String firstExperiment = progress.getFirst().experimentId();

    // At least one entity exists, so every step now reports what actually happened and the locks are
    // gone. A count of zero is still written as a sentence ("None registered yet") rather than as
    // "0 samples": a bare zero reads like a status, which is exactly what the empty state must not
    // do -- the difference being that here the step is reachable and the zero is a real result.
    rail.add(step(1, "Experiment", pluralise(progress.size(), "experiment created",
            "experiments created"),
        State.DONE, () -> navigate(ExperimentInformationMain.class, firstExperiment)));
    rail.add(step(2, "Samples", samples > 0
            ? pluralise(samples, "sample registered", "samples registered")
            : "None registered yet",
        samples > 0 ? State.DONE : State.TODO,
        () -> navigate(SampleInformationMain.class, firstExperiment)));
    // The stage's marker is a completeness ring: it carries the primary-data coverage at a glance,
    // with the detail beside it, instead of a warning triangle that only says "something is wrong".
    rail.add(stepWithMarker(
        primaryDatasetsRing(withData, measurements, missing),
        "Dataset",
        datasetDetail(withData, measurements, missing),
        missing > 0 ? State.INCOMPLETE : State.DONE,
        () -> navigate(MeasurementDatasetMain.class, firstExperiment)));
    return rail;
  }

  /** What the Dataset stage reports once at least one experiment exists. */
  private static String datasetDetail(int withData, int measurements, int missing) {
    if (measurements == 0) {
      return "None registered yet";
    }
    String total = pluralise(measurements, "measurement", "measurements");
    // Both branches are "N of M ...", so the two states read as the same measure with a different
    // ending -- and each stays grammatical for a single measurement.
    return missing == 0
        ? withData + " of " + total + " with data"
        : missing + " of " + total + " still need data";
  }

  private static CompletenessRing primaryDatasetsRing(int withData, int measurements, int missing) {
    CompletenessRing ring = new CompletenessRing(withData, measurements,
        measurements == 0
            ? "No measurements registered yet"
            : missing + " of " + measurements + " measurements still need primary data");
    ring.addClassName("project-progress-step-ring");
    return ring;
  }

  private static String pluralise(int count, String singular, String plural) {
    return count + " " + (count == 1 ? singular : plural);
  }

  private ListItem step(int ordinal, String label, String detail,
      State state, Runnable onOpen) {
    Span marker = new Span();
    marker.addClassName("project-progress-marker");
    switch (state) {
      case DONE -> marker.add(icon(VaadinIcon.CHECK));
      case INCOMPLETE -> marker.add(icon(VaadinIcon.INFO_CIRCLE));
      case ACTIVE -> marker.setText(Integer.toString(ordinal));
      case LOCKED -> marker.add(icon(VaadinIcon.LOCK));
      case TODO -> marker.setText(Integer.toString(ordinal));
    }
    return stepWithMarker(marker, label, detail, state, onOpen);
  }

  private ListItem stepWithMarker(Component marker, String label, String detail,
      State state, Runnable onOpen) {
    ListItem item = new ListItem();
    item.addClassName("project-progress-step");
    item.addClassName("project-progress-step--" + state.css());

    Div head = new Div();
    head.addClassName("project-progress-step-head");
    Span connector = new Span();
    connector.addClassName("project-progress-connector");
    head.add(marker, connector);

    Span labelSpan = new Span(label);
    labelSpan.addClassName("project-progress-step-label");
    Span detailSpan = new Span(detail);
    detailSpan.addClassName("project-progress-step-detail");
    item.add(head, labelSpan, detailSpan);
    if (onOpen != null) {
      item.addClassName("project-progress-step--link");
      Utility.activateOnClickAndKeys(item, "link", onOpen);
    }
    return item;
  }

  private void requestCreate() {
    // The owning page owns the creation workflow: this is the same request the experiments section
    // raises, so the two call-to-actions cannot drift into different destinations.
    fireEvent(new CreateExperimentRequestedEvent(this, true));
  }

  /**
   * Raised when the user asks to create an experiment from the progress band. The band does not
   * navigate itself; the page wires this and the experiments section to the same route.
   */
  public Registration addCreateExperimentListener(
      ComponentEventListener<CreateExperimentRequestedEvent> listener) {
    return addListener(CreateExperimentRequestedEvent.class, listener);
  }

  public static class CreateExperimentRequestedEvent extends
      ComponentEvent<ProjectProgressSection> {

    @Serial
    private static final long serialVersionUID = 1L;

    public CreateExperimentRequestedEvent(ProjectProgressSection source, boolean fromClient) {
      super(source, fromClient);
    }
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
    // No section-level link: it sat above both cards and only served one of them, so its scope was
    // never clear. The cards are clickable now and each names its own destination.
    head.add(label);

    Div grid = new Div();
    grid.addClassName("project-progress-datasets-grid");
    grid.add(primaryDataCard(progress));
    grid.add(connectedAssetsCard(connected));
    wrap.add(head, grid);
    return wrap;
  }

  /**
   * Primary data, as analysis sees it: how much of it there is, and where it sits.
   *
   * <p>This card answers "what data exists": a total, then where it sits per assay type. It
   * deliberately does NOT state coverage. "8 of 284 measurements" is a project-grain number that
   * hides which experiment is behind -- the question a reader actually has -- and it is already
   * answered at the right grain by the not-complete card and the experiment snapshot panels. The
   * file count that used to sit here is gone: it is not comparable across assay types, it cannot
   * be acted on, and pairing it with a byte total only invites the reader to divide the two (see
   * the migration's trap 4c).</p>
   */
  private Div primaryDataCard(List<ExperimentProgress> progress) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    card.add(cardTitle("Primary data — for analysis"));
    List<AssayTypeTotal> present = assayTypesWithPrimaryData(progress);
    card.add(cardFact(totalVolume(progress, present)));
    if (!present.isEmpty()) {
      card.add(cardFact(volumeByAssayType(present)));
    }
    // The card summarises the project's primary datasets, so it opens the page that lists them
    // one by experiment and assay type. It used to open the raw files of the first experiment,
    // which answered a narrower question and only ever for one of them.
    makeNavigable(card, "Open primary datasets", () -> getUI().ifPresent(
        ui -> ui.navigate(ProjectDatasetsMain.class, projectParameters())));
    return card;
  }

  /**
   * Makes a whole card the way into its surface, the way the rail steps are.
   *
   * <p>Role and keyboard handling come from the same helper the rail and the not-complete rows use, so
   * a card is reachable by Tab and activated by Enter or Space without a separate implementation
   * drifting from theirs.</p>
   */
  private static void makeNavigable(Div card, String description, Runnable action) {
    card.addClassName("project-progress-datasets-card--link");
    card.getElement().setAttribute("title", description);
    Utility.activateOnClickAndKeys(card, "link", action);
  }

  /**
   * The headline volume.
   *
   * <p>"Size not recorded" is a different statement from "0 B". A real project holds measurements
   * carrying primary data whose sizes the sync never recorded, and the read model cannot tell
   * that apart from empty data (migration trap 4d), so the card must not claim 0.</p>
   */
  private static String totalVolume(List<ExperimentProgress> progress, List<AssayTypeTotal> present) {
    if (present.isEmpty()) {
      return "No primary data attached yet";
    }
    long bytes = progress.stream().mapToLong(ExperimentProgress::primaryDataBytes).sum();
    return bytes > 0 ? Utility.humanReadableSize(bytes) + " in total" : "Size not recorded";
  }

  /** One assay type's share of the project's primary data. */
  private record AssayTypeTotal(AssayType type, int measurementsWithData, long bytes) {

    String label() {
      return MeasurementType.from(type).getType();
    }
  }

  /** The assay types that actually carry primary data, in declaration order. */
  private static List<AssayTypeTotal> assayTypesWithPrimaryData(List<ExperimentProgress> progress) {
    return Arrays.stream(AssayType.values())
        .map(type -> new AssayTypeTotal(type,
            progress.stream().mapToInt(entry -> entry.measurementsWithPrimaryData(type)).sum(),
            progress.stream().mapToLong(entry -> entry.primaryDataBytes(type)).sum()))
        .filter(total -> total.measurementsWithData() > 0)
        .toList();
  }

  /**
   * Where the volume sits, per assay type.
   *
   * <p>A type whose size was never recorded is named without a figure when nothing was measured,
   * or as "—" when its siblings were: the headline already states the gap, so a dash per type
   * would only repeat it, but a bare name beside figures would read as a zero.</p>
   */
  private static String volumeByAssayType(List<AssayTypeTotal> present) {
    boolean anyRecorded = present.stream().anyMatch(type -> type.bytes() > 0);
    return String.join(" · ", present.stream()
        .map(type -> anyRecorded
            ? type.label() + " " + sizeOrDash(type.bytes())
            : type.label())
        .toList());
  }

  /**
   * Size in the unit that keeps the number readable.
   *
   * <p>Zero means "not recorded" at this point in the flow; see {@link #volumeByAssayType}.</p>
   */
  private static String sizeOrDash(long bytes) {
    return bytes <= 0 ? "—" : Utility.humanReadableSize(bytes);
  }

  private Div connectedAssetsCard(List<ConnectedDatasetView> connected) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    makeNavigable(card, "Open connected datasets", () -> getUI().ifPresent(
        ui -> ui.navigate(ProjectDatasetsMain.class, projectParameters())));
    if (connected.isEmpty()) {
      card.add(cardTitle("Connected datasets — none, hosted elsewhere"));
      card.add(cardFact("None linked yet"));
      return card;
    }
    // A project's assets almost always come from one repository, so naming it on every row repeats
    // the same word three times beside the fields that actually differ. When they agree it moves
    // into the card title; when they do not, each row keeps its own.
    String sharedProvider = sharedProvider(connected);
    card.add(cardTitle(sharedProvider == null
        ? "Connected datasets — %d, hosted elsewhere".formatted(connected.size())
        : "Connected datasets — %d from %s".formatted(connected.size(), sharedProvider)));

    boolean perRowProvider = sharedProvider == null;
    Div rows = new Div();
    rows.addClassName("project-progress-connected-assets");
    if (perRowProvider) {
      rows.addClassName("project-progress-connected-assets--with-provider");
    }
    // Most recent activity first. The question this card answers is "is anything happening here",
    // and ordering by connection date buried the record that was synced this morning.
    connected.stream()
        .sorted(Comparator.comparing(ProjectProgressSection::lastActivity).reversed())
        .limit(3)
        .forEach(asset -> rows.add(connectedAssetRow(asset, perRowProvider)));
    card.add(rows);
    return card;
  }

  /**
   * The most recent activity on a connected dataset: its last sync if it has ever been synced,
   * otherwise the moment it was connected.
   *
   * <p>Both count as activity -- a record connected last year but synced this morning is fresh,
   * and that is what a reader scanning the card wants to know.</p>
   */
  private static Instant lastActivity(ConnectedDatasetView asset) {
    return asset.lastSyncedAt() != null ? asset.lastSyncedAt() : asset.connectedOn();
  }

  /**
   * Says which kind of activity the timestamp reports. "Connected" and "synced" are different
   * events, and a reader judging whether a record is current needs to know which one they are
   * looking at.
   */
  private static String activityLabel(ConnectedDatasetView asset) {
    return (asset.lastSyncedAt() != null ? "synced " : "connected ")
        + Utility.relativeTime(lastActivity(asset));
  }

  /**
   * The provider every connected asset agrees on, or null when they disagree or it is unknown.
   *
   * <p>Read from the whole list rather than the three rows on show: the card title describes the
   * project's assets, not the excerpt below it. Assets with no provider at all count as a
   * disagreement, so nothing is claimed that the data does not support.</p>
   */
  private static String sharedProvider(List<ConnectedDatasetView> connected) {
    List<String> providers = connected.stream()
        .map(ConnectedDatasetView::resourceProvider)
        .filter(provider -> provider != null && !provider.isBlank())
        .distinct()
        .toList();
    return providers.size() == 1 ? providers.getFirst() : null;
  }

  /**
   * One connected asset: what kind it is, its name, and -- when they differ -- where it is hosted.
   *
   * <p>The name is clamped, because a repository title runs to a full sentence, but it is clamped
   * on its own. Appending the provider to it put the two in one string, so the ellipsis ate the
   * provider first -- the single field that makes the three rows comparable -- and left the card
   * reading as an undifferentiated block of text. The full title stays in the tooltip.</p>
   *
   * <p>The kind is the repository's own vocabulary term (InvenioRDM resource types), not a label
   * invented here, and it is rendered with the same tag the connected-datasets page uses so that
   * "Dataset" or "Software" reads identically in both places.</p>
   */
  private static Div connectedAssetRow(ConnectedDatasetView asset, boolean showProvider) {
    Div row = new Div();
    row.addClassName("project-progress-connected-asset");

    // The type always occupies its column, even when the source gives none. A chip of varying
    // width prepended to the title pushed the titles out of line with each other, which defeats
    // the point of giving the type a column at all.
    Span typeCell = new Span();
    typeCell.addClassName("project-progress-connected-asset-type");
    String resourceType = asset.resourceType();
    if (resourceType != null && !resourceType.isBlank()) {
      typeCell.add(DataSetTagFactory.create(TagType.DATA_SET_TYPE, resourceType));
    }
    row.add(typeCell);

    Span title = new Span(asset.title());
    title.addClassName("project-progress-connected-asset-title");
    title.getElement().setAttribute("title", asset.title());
    row.add(title);

    Span activity = new Span(activityLabel(asset));
    activity.addClassName("project-progress-connected-asset-activity");
    row.add(activity);

    String provider = asset.resourceProvider();
    if (showProvider && provider != null && !provider.isBlank()) {
      Span providerSpan = new Span(provider);
      providerSpan.addClassName("project-progress-connected-asset-provider");
      row.add(providerSpan);
    }
    return row;
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
    DONE("done"), INCOMPLETE("incomplete"), ACTIVE("active"), LOCKED("locked"), TODO("todo");

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
