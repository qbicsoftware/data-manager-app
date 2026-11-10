package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.logging.api.Logger;
import life.qbic.datamanager.views.general.CompletenessRing;
import life.qbic.datamanager.views.general.snapshot.SnapshotPanel;
import life.qbic.datamanager.views.general.DataSetTagFactory;
import life.qbic.datamanager.views.general.DataStateLabel;
import life.qbic.datamanager.views.general.DataSetTagFactory.TagType;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.datasets.PrimaryDatasetsComponent;
import life.qbic.datamanager.views.projects.project.datasets.ProjectDatasetsMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.AssayStanding;
import life.qbic.projectmanagement.application.query.DataState;
import life.qbic.projectmanagement.application.query.AssayType;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import reactor.core.publisher.Mono;

/**
 * "Where the project stands" — the prototype's progress band.
 *
 * <p>Structure (top to bottom): the band title; the progress card with a headline sentence, a rail
 * of three stages ({@code Experiment created -> Samples registered -> Dataset}) on a connector
 * line, and the dataset standing (primary data and connected assets).</p>
 *
 * <p><b>The per-experiment deficit is not stated here.</b> It used to open the band as a tinted card
 * and later closed it as a disclosure, and both were answering a question this band is not placed to
 * answer. The band reports the project's standing; "which experiments are behind" is per-experiment
 * detail, it is already carried at the right grain by the experiment entries below the band and by
 * the Datasets page, and the identity band's chip links straight to that detail. Keeping a third
 * rendering of it here cost a block of vertical space and restated numbers the rail and the dataset
 * card already state.</p>
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

  private static final Logger log = logger(ProjectProgressSection.class);

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  private final transient AssociatedDatasetService associatedDatasetService;
  private final transient ProjectQueryService projectQueryService;
  private final Div content = new Div();
  private String projectId;
  /** Resolved before the progress render, since the dataset card needs both. */
  private List<AssayStanding> assayStanding = List.of();

  public ProjectProgressSection(AssociatedDatasetService associatedDatasetService,
      ProjectQueryService projectQueryService) {
    this.associatedDatasetService = requireNonNull(associatedDatasetService,
        "associatedDatasetService must not be null");
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
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
    // The dataset card is backed by its own read model, resolved once per load. A failure there must
    // not take the progress band down with it: the band is the page's primary job and its data is
    // already in hand, so a failed assay query degrades to "no datasets stated" rather than to an
    // error page.
    Mono<List<AssayStanding>> assays = projectQueryService.getAssayStanding(projectId)
        .collectList()
        .onErrorResume(error -> {
          // The facade takes a message and no cause, so the exception is named in the text rather
          // than attached: losing the stack trace here is preferable to failing the whole band over
          // a degraded card.
          log.warn("Assay standing could not be loaded for project " + projectId + ": "
              + error.getClass().getSimpleName() + " (" + error.getMessage() + ")");
          return Mono.just(List.of());
        });
    ReactiveUiBridge.onUi(ui, progress.zipWith(assays),
        pair -> {
          assayStanding = pair.getT2();
          render(pair.getT1(), overview, connected);
        },
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

    // The overview leads and the deficit follows it — see the class note on why the order is not
    // the other way round.
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
    // "Done" means every experiment has samples, not that the stage has been reached. The rail used
    // to mark this stage done as soon as one experiment had a sample, so it showed a green check
    // beside a headline reading "4 of 9 experiments have registered samples" -- a contradiction the
    // reader has to resolve. Some is Partial; none is Todo.
    long withSamples = progress.stream().filter(entry -> entry.sampleCount() > 0).count();
    rail.add(step(2, "Samples", samples > 0
            ? pluralise(samples, "sample registered", "samples registered")
            : "None registered yet",
        samples == 0 ? State.TODO
            : withSamples == progress.size() ? State.DONE : State.PARTIAL,
        () -> navigate(SampleInformationMain.class, firstExperiment)));
    // The Dataset stage keeps the completeness ring as its marker. This is deliberate for now: the
    // ring is in use on three other surfaces and the rail is the only place it meets plain discs, so
    // the page is being used to compare the two encodings side by side -- the ring here, the bar in
    // the Primary data card below. The ring also states its share in the detail slot, so neither the
    // marker nor the sentence carries the proportion alone.
    rail.add(stepWithMarker(
        primaryDatasetsRing(withData, measurements, missing),
        "Dataset",
        datasetDetail(withData, measurements, missing),
        switch (DataState.of(withData, measurements)) {
          case COMPLETE -> State.DONE;
          case WAITING -> State.TODO;
          // Partial is the interesting one, and it is neither: the ring already shows how far along
          // it is, so the marker carries "in progress" without claiming success or failure.
          case PARTIAL, AT_RISK -> State.PARTIAL;
        },
        () -> navigate(MeasurementDatasetMain.class, firstExperiment)));
    return rail;
  }

  /**
   * What the Dataset stage reports once at least one experiment exists.
   *
   * <p>One number, one word -- the same grammar as the two stages beside it ("599 samples
   * registered"). It used to read "8 of 284 measurements with data · 276 missing", which put three
   * figures in the slot where its neighbours put one, and made the reader reconcile a numerator, a
   * denominator and a complement against a ring that already showed the proportion and a footnote
   * that already named the outstanding count.</p>
   *
   * <p>The word is "missing" rather than "still need primary data" because the ring beside it is
   * labelled and the footnote below spells the condition out in full; this slot only has to say how
   * much is outstanding.</p>
   */
  private static String datasetDetail(int withData, int measurements, int missing) {
    if (measurements == 0) {
      return "None registered yet";
    }
    return missing == 0
        ? pluralise(withData, "measurement with data", "measurements with data")
        : pluralise(missing, "measurement missing data", "measurements missing data");
  }

  /**
   * The Dataset stage's marker: primary-data coverage at a glance, sharing the slot with the two
   * plain discs beside it.
   *
   * <p>An arc is weak at low values, and this project's coverage is 3%. The ring's own track is what
   * makes that readable -- it frames the arc as mostly-empty rather than letting a sliver float in
   * white -- so the track is drawn at a contrast that survives a 3% arc, and the detail beside the
   * marker states the figures in words.</p>
   */
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
      // Partial is marked with a diagonal split instead of a glyph: a tick would claim completion and
      // an info circle would claim a problem, and it is neither.
      case PARTIAL -> marker.addClassName("project-progress-marker--partial");
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
    grid.add(primaryDataCard(assayStanding));
    grid.add(connectedAssetsCard(connected));
    wrap.add(head, grid);
    return wrap;
  }

  /** How many analysis-method rows the card shows before deferring to the Datasets page. */
  private static final int ASSAY_ROWS_SHOWN = 4;

  /**
   * The project's primary datasets: one line per analysis method, grouped by omics domain.
   *
   * <p><b>What a dataset is.</b> The data of <em>one experiment</em> for <em>one analysis method</em>.
   * It is not declared by anyone: it is the consequence of measurements having been done. Two
   * experiments measured as WES are two datasets; one experiment measured as WES and RNA-Seq is also
   * two. Its type is the analysis method, because that is what a reader picks datasets by when
   * planning an analysis -- variant calling, cluster analysis, ... -- and the omics domain alone is
   * far too coarse to do that: WES, RNA-Seq and 16S are all genomics and are not interchangeable.</p>
   *
   * <p><b>The card states no total, and that is deliberate.</b> A measurement measuring samples with
   * different analysis methods is attributed to each (ADR 0010), so summing the rows of this card can
   * exceed the measurement total the rail states a few lines above -- on one real project the rows
   * summed to 287 against the rail's 284. Both numbers are correct, but a reader comparing them 30px
   * apart concludes the page is broken. The card therefore reports per method and never aggregates:
   * there is no total here to disagree with the rail.</p>
   *
   * <p><b>The row states a state, not an arithmetic problem.</b> It used to read "2 of 86
   * measurements  84 missing" -- three numbers, 84 being 86-2, an implicit subtraction the reader was
   * silently asked to check. It now says what the two cases actually are: "Complete", or how many are
   * outstanding out of how many. The ring carries the proportion, so the text does not repeat it.</p>
   *
   * <p><b>Bounded.</b> Rows are capped at {@value #ASSAY_ROWS_SHOWN}. Analysis methods are not a
   * small set -- a project running several NGS assays alongside proteomics and immunopeptidomics has
   * a dozen -- and an uncapped list made this card taller than the rail and the disclosure together,
   * which is the same domination the "Not complete yet" card had before it was demoted.</p>
   */
  /**
   * Package-private so a spec can assert the table without a {@code UI}: {@code reload()} returns
   * early when there is no current UI, which would otherwise make the card unreachable from a plain
   * unit test.
   */
  Div primaryDataCard(List<AssayStanding> datasets) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    // The heading links to <em>its own band</em> on the Datasets page, not to the page top. Both
    // cards sit side by side and both lead to the same page, so a link without a target dropped the
    // reader at the top of a page whose first band is the other card's -- clicking "Linked datasets"
    // and clicking "Measurement datasets" produced an identical landing.
    card.add(cardTitle("Measurement datasets", PrimaryDatasetsComponent.BAND_ANCHOR));

    if (datasets.isEmpty()) {
      card.add(cardFact(
          "None yet — a dataset appears once an experiment has measurements"));
      return card;
    }

    List<AssayRollup> rollup = assayRollup(datasets);
    // The count line the linked card carries, in the same position and the same wording. Only the
    // linked card stated its size ("6 records"), so the pair read as two different kinds of block:
    // one that tells you how much it holds and one that does not.
    card.add(cardSummary(rollup.size(), "dataset", "datasets"));
    card.add(assayTable(rollup));
    // One disclosure grammar for the whole page: the same footer the snapshot panels use, right
    // aligned under a hairline and in the link colour. It used to be an inert lowercase sentence,
    // which named a destination while the identical destination sat 20px above as a link.
    card.add(cardFooter(rollup.size() > ASSAY_ROWS_SHOWN
        ? "All %d datasets".formatted(rollup.size())
        : "All %d %s".formatted(rollup.size(),
            rollup.size() == 1 ? "dataset" : "datasets"),
        AppRoutes.ProjectRoutes.DATASETS.formatted(projectId)
            + "#" + PrimaryDatasetsComponent.BAND_ANCHOR));
    return card;
  }

  /**
   * The card's datasets as one table: a header row, then a row per dataset.
   *
   * <p><b>It is the same table the experiment panels render</b> (see
   * {@link life.qbic.datamanager.views.general.snapshot.SnapshotPanel}), because it answers the same
   * question at the same grain. Three things follow from that, and each fixes a defect the previous
   * per-domain layout had:</p>
   *
   * <ul>
   *   <li><b>The domain is a column, not a caption per row.</b> The card used to emit one uppercase
   *       caption per domain. With one assay per domain that spent six lines on three facts, and the
   *       caption was wider than the single row it governed. As a column with a header it repeats
   *       like data rather than like decoration — which is what lets the card hold a dozen assays
   *       across three domains without turning into a stack of headings.</li>
   *   <li><b>The header row attributes the right-hand numbers.</b> Without it they sat across a
   *       ~180px void from the method with nothing saying what they measured, so each row read as
   *       two unrelated islands.</li>
   *   <li><b>Two counts in different bases are named.</b> "1 dataset" beside "1 of 6 with data" put
   *       a dataset count and a measurement count next to each other with no marker that the base
   *       changed. The headers now say which is which.</li>
   * </ul>
   *
   * <p>The state is a pill in the shared vocabulary, so "Partial" here is the word the panels below
   * and the Datasets page use. The phrase "Partially available" both duplicated the pill and stated
   * the proportion a second time.</p>
   */
  // Package-private and static so a spec can assert the table without building the section: the
  // table reads only its argument, and constructing the section needs collaborators this classpath
  // cannot mock.
  static Div assayTable(List<AssayRollup> rollup) {
    Div table = new Div();
    table.addClassName("project-progress-assays");
    table.addClassName("project-progress-assays--5-columns");

    // Bounded before rendering, so the cap applies to dataset rows rather than to whole domains.
    table.add(assayCells(new String[]{"Omics domain", "Method", "Datasets",
        "Measurements with data", "Availability"}, true, null));
    rollup.stream().limit(ASSAY_ROWS_SHOWN).forEach(assay -> {
      DataState state = DataState.of(assay.withData(), assay.measurements());
      table.add(assayCells(new String[]{
          MeasurementType.from(assay.domain()).getType(),
          assay.methodLabel(),
          Integer.toString(assay.datasets()),
          assay.measurements() == 0 ? "none" : "%d of %d".formatted(assay.withData(),
              assay.measurements()),
          DataStateLabel.word(state)},
          false, state));
    });
    return table;
  }

  /**
   * One row's cells, as items of the table's single grid.
   *
   * <p>A spacer cell precedes the numbers: it absorbs the free space so the identity group (domain,
   * method) stays tight on the left and the numbers keep the right edge, rather than the gap opening
   * in the middle of the row. The grid declares a matching track, derived from the same rule — a
   * row with more cells than tracks shifts right by auto-placement, a bug this layout has had.</p>
   */
  private static Div assayCells(String[] cells, boolean header, DataState state) {
    Div row = new Div();
    row.addClassName("project-progress-assay-row");
    // The header emits the spacer too. Skipping it for the header put its five labels into the
    // first five tracks of a six-track grid, so "Measurements with data" sat under the spacer and
    // the whole header row was off by one column against every row beneath it.
    int spacerAt = cells.length > 3 ? cells.length - 2 : -1;
    for (int index = 0; index < cells.length; index++) {
      if (index == spacerAt) {
        Span spacer = new Span();
        spacer.addClassName("project-progress-assay-spacer");
        row.add(spacer);
      }
      Span cell = new Span(cells[index]);
      if (header) {
        cell.addClassName("project-progress-assay-header");
      } else if (index == cells.length - 1 && state != null) {
        // The state is a pill in the same vocabulary the panels below use, so one word holds across
        // the whole page and colour varies with the state instead of everything being info-blue.
        cell.addClassNames("snapshot-panel-state",
            "snapshot-panel-state--" + DataStateLabel.tone(state));
      } else if (index == 0) {
        cell.addClassName("project-progress-assay-domain-cell");
      } else if (index == 1) {
        cell.addClassName("project-progress-assay-method");
      } else {
        cell.addClassName("project-progress-assay-number");
      }
      row.add(cell);
    }
    return row;
  }

  /**
   * One analysis method's datasets across the project, summed.
   *
   * <p>Summed across experiments on purpose: the project grain answers "how much WES does this
   * project hold", and the per-experiment split is one click away. The count of datasets is carried
   * separately, because two experiments measured as WES are two datasets of one method -- a
   * distinction the method name alone loses.</p>
   */
  record AssayRollup(String methodLabel, AssayType domain, int datasets, int measurements,
                     int withData, long bytes) {

  }

  /**
   * Rolls the per-dataset rows up to one entry per (domain, method label), in domain then label order.
   *
   * <p>Grouped by the pair, not by the label alone: the label is free text for PXP/IP, so two domains
   * could otherwise produce the same key. Ordered by domain so the grouping reads as groups, then by
   * the label, which is the stable comparable form the view resolved ("WES", "CIDOT").</p>
   */
  static List<AssayRollup> assayRollup(List<AssayStanding> datasets) {
    return datasets.stream()
        .collect(Collectors.groupingBy(
            dataset -> Map.entry(dataset.assayType(), dataset.methodLabel()),
            LinkedHashMap::new, Collectors.toList()))
        .entrySet().stream()
        .map(entry -> new AssayRollup(
            entry.getKey().getValue(),
            entry.getKey().getKey(),
            entry.getValue().size(),
            entry.getValue().stream().mapToInt(AssayStanding::measurements).sum(),
            entry.getValue().stream().mapToInt(AssayStanding::withPrimaryData).sum(),
            entry.getValue().stream().mapToLong(AssayStanding::primaryDataBytes).sum()))
        .sorted(Comparator.comparing((AssayRollup rollup) -> rollup.domain().ordinal())
            .thenComparing(AssayRollup::methodLabel))
        .toList();
  }

  /**
   * Makes a whole card the way into its surface, the way the rail steps are.
   *
   * <p>Role and keyboard handling come from the same helper the rail and the not-complete rows use, so
   * a card is reachable by Tab and activated by Enter or Space without a separate implementation
   * drifting from theirs.</p>
   */
  /**
   * The project's linked datasets: external records the user attached.
   *
   * <p><b>The card is not a click target.</b> Each record has its own destination, so following the
   * rule that a container stays inert once it holds more than one link: the card would otherwise be
   * a link containing links. Each record's title links to the record, and the card's heading links to
   * the full list.</p>
   */
  /** How many linked records the card shows before deferring to the Datasets page. */
  private static final int CONNECTED_ROWS_SHOWN = 3;

  private Div connectedAssetsCard(List<ConnectedDatasetView> connected) {
    Div card = new Div();
    card.addClassName("project-progress-datasets-card");
    // The count and the provider are the card's headline fact, so they lead in body weight and the
    // repository is set apart in secondary text -- rather than one em-dash construction per card,
    // which made both cards read as a caption with a fact bolted onto it.
    if (connected.isEmpty()) {
      card.add(cardTitle("Linked datasets"));
      card.add(cardFact("None linked yet"));
      return card;
    }
    // A project's assets almost always come from one repository, so naming it on every row repeats
    // the same word three times beside the fields that actually differ. When they agree it moves
    // into the card title; when they do not, each row keeps its own.
    String sharedProvider = sharedProvider(connected);
    // The heading is the card's single way into the full list; the rows keep their own links.
    card.add(cardTitle("Linked datasets", PrimaryDatasetsComponent.CONNECTED_BAND_ANCHOR));

    // The count is a fact about the card, not part of its name. "Linked datasets 6" set a number at
    // heading weight inside a title, stated the same quantity three ways (6 / three rows / "and 3
    // more") and had no counterpart on the card beside it. It now leads the body the way the
    // measurement card's table does, in the same weight as that card's content.
    card.add(cardSummary(connected.size(), "record", "records", sharedProvider));

    boolean perRowProvider = sharedProvider == null;
    Div rows = new Div();
    rows.addClassName("project-progress-connected-assets");
    if (perRowProvider) {
      rows.addClassName("project-progress-connected-assets--with-provider");
    }
    // A header row, the way the measurement table beside it has one. Without it "3 days ago" and
    // "Zenodo" ran on from the title as if they were part of it, and the reader had to infer what
    // the two trailing columns meant.
    rows.add(connectedHeaderRow(perRowProvider));
    // Most recent activity first. The question this card answers is "is anything happening here",
    // and ordering by connection date buried the record that was synced this morning.
    connected.stream()
        .sorted(Comparator.comparing(ProjectProgressSection::lastActivity).reversed())
        .limit(CONNECTED_ROWS_SHOWN)
        .forEach(asset -> rows.add(connectedAssetRow(asset, perRowProvider)));
    card.add(rows);

    // Same disclosure grammar as the card beside it and the panels below: a footer link under a
    // hairline, not an inert lowercase sentence that names a destination.
    card.add(cardFooter(connected.size() > CONNECTED_ROWS_SHOWN
            ? "All %d records".formatted(connected.size())
            : "All %d %s".formatted(connected.size(),
                connected.size() == 1 ? "record" : "records"),
        AppRoutes.ProjectRoutes.DATASETS.formatted(projectId) + "#"
            + PrimaryDatasetsComponent.CONNECTED_BAND_ANCHOR));
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
  static String activityLabel(ConnectedDatasetView asset) {
    // Just the relative time. The verb ("synced" / "connected") distinguished two events that this
    // card does not act on: it is answering "is anything happening here", and when it last happened
    // is the whole answer. The distinction survives where it matters -- the two are still ordered by
    // their last activity, and the record's own page states which event that was.
    return Utility.relativeTime(lastActivity(asset));
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
  /**
   * The linked table's header row, in the same grammar as the measurement table's.
   *
   * <p>"Last activity" rather than "Synced"/"Connected": the column reports when the record last
   * changed, and which of the two events that was is not something this card acts on.</p>
   */
  static Div connectedHeaderRow(boolean showProvider) {
    Div row = new Div();
    row.addClassName("project-progress-connected-asset");
    for (String label : showProvider
        ? new String[]{"Type", "Name", "Last activity", "Source"}
        : new String[]{"Type", "Name", "Last activity"}) {
      Span header = new Span(label);
      header.addClassName("project-progress-connected-asset-header");
      row.add(header);
    }
    return row;
  }

  static Div connectedAssetRow(ConnectedDatasetView asset, boolean showProvider) {
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

    // Each record is its own destination, so the title is the link. A record whose source gave no
    // URL falls back to the plain span: a link that goes nowhere is worse than text.
    Component title;
    if (asset.accessLink() != null && !asset.accessLink().isBlank()) {
      Anchor link = new Anchor(asset.accessLink(), asset.title());
      link.setTarget(AnchorTarget.BLANK);
      link.addClassName("project-progress-connected-asset-title");
      link.getElement().setAttribute("title", asset.title());
      title = link;
    } else {
      Span plain = new Span(asset.title());
      plain.addClassName("project-progress-connected-asset-title");
      plain.getElement().setAttribute("title", asset.title());
      title = plain;
    }
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

  /**
   * A card heading that is the card's way into the matching band of the Datasets page.
   *
   * <p>The fragment is not decoration. The page holds two bands and this section holds two cards,
   * both pointing at that page: without a target they landed identically at the top, so "Linked
   * datasets" put the reader in front of the primary datasets and left them to scroll. The fragment
   * names the band, the band carries the matching id, and {@code ProjectDatasetsMain} turns the two
   * into an actual scroll once the (asynchronous) band content exists.</p>
   *
   * <p>A real anchor built from an application path, for the reason {@link Utility#textLink}
   * documents: {@code RouterLink} + {@code RouteParameters} silently renders an inert element when
   * the parameters do not bind, and a fragment cannot be expressed on a {@code RouterLink} at all.</p>
   */
  private Div cardTitle(String text, String anchor) {
    Div title = new Div();
    title.addClassName("project-progress-datasets-card-title");
    // The same glyph the snapshot panels carry, so the two cards and the two panels below name their
    // subject the same way: a database for stored data, a flask for samples.
    title.add(surfaceIcon(text));
    title.add(Utility.textLink(text,
        AppRoutes.ProjectRoutes.DATASETS.formatted(projectId) + "#" + anchor,
        "project-progress-datasets-card-title-link"));
    return title;
  }

  /** A card heading that does not navigate. */
  private static Div cardTitle(String text) {
    Div title = new Div();
    title.addClassName("project-progress-datasets-card-title");
    title.add(surfaceIcon(text));
    title.add(new Span(text));
    return title;
  }

  /**
   * The card's count line: how much it holds, and optionally where that comes from.
   *
   * <p>Both cards carry it, in the same place and the same wording. Only the linked card stated its
   * size before, so the pair read as two different kinds of block -- one that tells you how much it
   * holds and one that does not -- even though they answer the same question at the same grain.</p>
   */
  static Div cardSummary(int count, String singular, String plural) {
    return cardSummary(count, singular, plural, null);
  }

  static Div cardSummary(int count, String singular, String plural, String qualifier) {
    Div summary = new Div();
    summary.addClassName("project-progress-datasets-summary");
    summary.add(new Span(SnapshotPanel.pluralise(count, singular, plural)));
    if (qualifier != null && !qualifier.isBlank()) {
      Span qualifierSpan = new Span("from %s".formatted(qualifier));
      qualifierSpan.addClassName("project-progress-datasets-summary-provider");
      summary.add(qualifierSpan);
    }
    return summary;
  }

  /**
   * The decorative glyph naming a card's subject.
   *
   * <p>Decoration only — the title carries the meaning, so it is {@code aria-hidden}. Mirrors the
   * snapshot panels' glyph so a reader meets one visual language for "stored data" across the page.</p>
   */
  private static Icon surfaceIcon(String label) {
    Icon glyph = label.startsWith("Linked") || label.startsWith("Measurement")
        ? VaadinIcon.DATABASE.create()
        : VaadinIcon.RECORDS.create();
    glyph.addClassName("project-progress-datasets-card-icon");
    glyph.getElement().setAttribute("aria-hidden", "true");
    return glyph;
  }

  /**
   * A card's way out, below a hairline, in the same grammar as the snapshot panels' footer.
   *
   * <p>Both cards used to disclose their cap as an inert lowercase sentence ("and 3 more records on
   * the datasets page") while the identical destination sat 20px above as a link, and the panels
   * below used a right-aligned footer link. One grammar now covers all four surfaces.</p>
   */
  private Div cardFooter(String text, String href) {
    Div footer = new Div();
    footer.addClassName("project-progress-datasets-card-footer");
    footer.add(Utility.textLink(text + " \u2192", href,
        "project-progress-datasets-card-footer-link"));
    return footer;
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
    DONE("done"),
    /** Started but not finished: neither success nor absence. */
    PARTIAL("partial"),
    INCOMPLETE("incomplete"),
    ACTIVE("active"),
    LOCKED("locked"),
    TODO("todo");

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
