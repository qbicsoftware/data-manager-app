package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.logging.api.Logger;
import life.qbic.datamanager.views.general.snapshot.SnapshotPanel;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentDesignMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentSetupMain;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentalVariableInformation;
import life.qbic.projectmanagement.application.query.AssayStanding;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

/**
 * Experiment overview — the experiment's landing and hub.
 *
 * <p>The design leads, read-only, followed by the two data surfaces it scopes (Samples, Dataset) as
 * snapshot cards. The cards are the navigation: each states what it holds and what is next and opens
 * its surface. Editing the design is a separate route, reached from the design band
 * (project owner, 2026-10-19).</p>
 *
 * @since 1.19.0
 */
@SpringComponent
@UIScope
public class ExperimentOverviewComponent extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger log = logger(ExperimentOverviewComponent.class);

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  private final transient ProjectQueryService projectQueryService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient ConfoundingVariableService confoundingVariableService;
  private final transient SampleInformationService sampleInformationService;
  private final Div content = new Div();
  private Context context;

  public ExperimentOverviewComponent(
      @Autowired ProjectQueryService projectQueryService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired ConfoundingVariableService confoundingVariableService,
      @Autowired SampleInformationService sampleInformationService) {
    this.projectQueryService = requireNonNull(projectQueryService);
    this.experimentInformationService = requireNonNull(experimentInformationService);
    this.confoundingVariableService = requireNonNull(confoundingVariableService);
    this.sampleInformationService = requireNonNull(sampleInformationService);
    addClassName("experiment-overview");
    content.addClassName("experiment-overview-content");
    add(content);
  }

  public void setContext(Context context) {
    this.context = requireNonNull(context, "context must not be null");
    reload();
  }

  private void reload() {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    String projectId = context.projectId().orElseThrow().value();
    ExperimentId experimentId = context.experimentId().orElseThrow();

    content.removeAll();

    // The experiment is the read model for the setup band: its name and its origins. Binding the
    // result instead of discarding it is the point -- the band used to state what setup was for and
    // then show nothing, so the only way to learn what was actually configured was to open the edit
    // route.
    Experiment experiment = experimentInformationService.find(projectId, experimentId)
        .orElseThrow();
    List<ExperimentalVariableInformation> variables =
        experimentInformationService.getVariablesOfExperiment(projectId, experimentId);
    List<ExperimentalGroup> groups = experimentInformationService.experimentalGroupsFor(projectId,
        experimentId);
    List<ConfoundingVariableInformation> confounding =
        confoundingVariableService.listConfoundingVariablesForExperiment(projectId,
            new ExperimentReference(experimentId.value()));
    // Registered samples per experimental group, keyed by group id. The group's own sampleSize() is
    // the planned number of biological replicates, which users do not read as "samples", so the
    // overview reports what is actually registered per condition instead.
    Map<Long, Long> samplesPerGroup = sampleInformationService
        .retrieveSamplesForExperiment(context.projectId().orElseThrow(), experimentId.value())
        .stream()
        .filter(sample -> sample.experimentalGroupId() != null)
        .collect(Collectors.groupingBy(Sample::experimentalGroupId, Collectors.counting()));

    content.add(pageHeader(experiment));
    content.add(setupBand(experiment));
    content.add(designBand(variables, groups, confounding, samplesPerGroup));

    Div dataBand = new Div();
    dataBand.addClassName("experiment-overview-band");
    Div dataHead = new Div();
    dataHead.addClassName("experiment-overview-band-head");
    dataHead.add(sectionTitle("Data at a glance"),
        hint("What is registered, and what to do next."));
    dataBand.add(dataHead);
    Div cards = new Div();
    cards.addClassName("experiment-overview-cards");
    cards.add(message("Loading data …"));
    dataBand.add(cards);
    content.add(dataBand);

    // The dataset card states one row per dataset, so it needs the dataset-grain read model (one
    // AssayStanding per experiment/domain/method/instrument) -- the progress row only knows omics
    // domains, which can back several datasets. It is the same read model the project overview and
    // the Datasets page consume (ADR 0010), so the three surfaces cannot disagree.
    //
    // A failed assay query must not take the two cards down with it, and it must not be rendered as
    // "no datasets" either: those are opposite statements, and the silent version of this hid a
    // broken query once already. The failure is logged and the panel says so.
    Mono<AssayLoad> assays = projectQueryService.getAssayStanding(projectId)
        .collectList()
        .map(rows -> new AssayLoad(rows.stream()
            .filter(row -> row.experimentId().equals(experimentId.value()))
            .toList(), false))
        .onErrorResume(error -> {
          log.error("Assay standing could not be loaded for project " + projectId + ": "
              + error.getClass().getSimpleName() + " (" + error.getMessage() + ")", error);
          return Mono.just(new AssayLoad(List.of(), true));
        });

    Mono<Data> data = Mono.zip(
            projectQueryService.getExperimentProgress(projectId, experimentId.value()),
            projectQueryService.getRecentSamples(projectId, experimentId.value()).collectList(),
            projectQueryService.getRecentMeasurements(projectId, experimentId.value()).collectList(),
            assays)
        .map(values -> new Data(values.getT1(), values.getT2(), values.getT3(), values.getT4()));
    ReactiveUiBridge.onUi(ui, data, snapshot -> renderCards(cards, snapshot), error -> {
      cards.removeAll();
      cards.add(message("Data could not be loaded."));
    });
  }

  /**
   * The experiment's setup surface: its name and the sample origins it derives from, read-only.
   *
   * <p>Split out of the design band when the two surfaces became separate routes. It reports the
   * configured values rather than only naming the surface, because the overview is the one place a
   * user can see an experiment's state without opening an editor: a band that showed nothing but an
   * "Edit setup" button forced a trip to the edit route just to find out what was already there.
   * The three origins are the registration template's Species/Specimen/Analyte options, so this is
   * also where a user checks whether the template will offer what they expect.</p>
   *
   * <p>Confounding variables are reported by the design band, since they are experiment design
   * rather than sample annotation.</p>
   *
   * <p>Package-private so a spec can assert what this read-only surface reports without needing a
   * {@code UI}: {@link #reload()} returns early when there is no current UI, which would otherwise
   * make the band unreachable from a plain unit test.</p>
   */
  Div setupBand(Experiment experiment) {
    Div band = new Div();
    band.addClassName("experiment-overview-band");

    Div head = new Div();
    head.addClassName("experiment-overview-band-head");
    // The action belongs to the band, not to whatever content happens to be last, so it sits with the
    // heading it addresses (top-right). Trailing the content made it read as a continuation of the
    // values above it, and its position moved with the content's height.
    //
    // The purpose text rides in the head as a hint, the way the "Data at a glance" band already does
    // it. On its own full-width row it cost a whole line of vertical space before any value was
    // shown, on a page whose point is the values.
    head.add(sectionTitle("Setup"),
        hint("The origins become the Species, Specimen and Analyte options in the "
            + "registration template."),
        actions(action("Edit setup", this::navigateToSetup)));
    band.add(head);
    band.add(setupDetailGrid(experiment));

    return band;
  }

  /**
   * The band's configured values. Split from {@link #setupBand(Experiment)} so both can be tested:
   * the grid's contents independently of the band, which is what the wiring regression was.
   */
  static Div setupDetailGrid(Experiment experiment) {
    Div grid = new Div();
    grid.addClassName("experiment-overview-grid");
    // No "Experiment name" block: the name is the page subtitle now, and repeating it here would
    // print it three times on one screen (breadcrumb, header, band) while making this grid four
    // columns against the design band's three.
    grid.add(detailBlock("Species", originEntries(experiment.getSpecies())));
    grid.add(detailBlock("Specimen", originEntries(experiment.getSpecimens())));
    grid.add(detailBlock("Analyte", originEntries(experiment.getAnalytes())));
    return grid;
  }

  /**
   * The experiment's name, as the header subtitle.
   *
   * <p>The {@code Experiment} constructor and {@code setName} both reject an empty name, so a loaded
   * experiment always has one; the empty case is defended rather than exercised. It yields no
   * subtitle instead of an empty paragraph, because a stray blank line under the H1 would read as a
   * rendering fault rather than as a state.</p>
   */
  private static List<String> nameEntries(Experiment experiment) {
    String name = experiment.getName();
    return name == null || name.isBlank() ? List.of() : List.of(name);
  }

  /**
   * The labels of the ontology terms chosen as sample origins.
   *
   * <p>Reads the label, not the term's {@code toString}: the label is what the user picked and what
   * the registration template offers, whereas the full term also carries the ontology and accession
   * and is too long for this grid. An empty result renders as "None yet" via
   * {@link #detailBlock}, which is the honest statement for an experiment whose origins have not
   * been set.</p>
   */
  private static List<String> originEntries(Collection<OntologyTerm> terms) {
    return terms.stream()
        .filter(Objects::nonNull)
        .map(OntologyTerm::getLabel)
        .toList();
  }

  private Div designBand(List<ExperimentalVariableInformation> variables,
      List<ExperimentalGroup> groups, List<ConfoundingVariableInformation> confounding,
      Map<Long, Long> samplesPerGroup) {
    Div band = new Div();
    band.addClassName("experiment-overview-band");
    // Keyed off experimental groups, not off "variables OR groups". Groups are what actually gates
    // sample registration (SampleInformationMain refuses to open the registration dialog while
    // getExperimentalGroups() is empty), so the previous predicate claimed "Defined" for a
    // variables-only design at the exact moment registration was still refused. Variables are an
    // enabling prerequisite for groups rather than a gate of their own -- a group condition picks
    // one level per variable, so levels cannot exist without them -- and they are surfaced below as
    // the first of two ordered steps instead of as a second, false milestone.
    boolean registrationUnlocked = !groups.isEmpty();

    Div head = new Div();
    head.addClassName("experiment-overview-band-head");
    // One action slot with a state-dependent label: "Define design" while the band is incomplete
    // (the action is what unlocks registration), "Edit design" once it is not. Both sit with the
    // heading, so the action does not jump around the band as the state changes.
    head.add(sectionTitle("Design"),
        stateTag(registrationUnlocked ? "Groups defined" : "Incomplete",
            registrationUnlocked ? "done" : "active"),
        actions(action(registrationUnlocked ? "Edit design" : "Define design",
            this::navigateToDesign)));
    band.add(head);

    if (!registrationUnlocked) {
      // The two steps are ordered because the order is the explanation: a condition is a choice of
      // levels per variable, so variables come first. Neither is presented as optional, and the
      // second names what it unlocks -- that is the prerequisite the user needs to know about,
      // since nothing on this page can be filled in until it is met.
      band.add(note("Sample registration stays blocked until this experiment has at least one "
          + "experimental group, because every registered sample is assigned to a group. "
          + "Groups are built in two ordered steps:"));
      band.add(designSteps(variables, groups));
      return band;
    }

    Div grid = new Div();
    grid.addClassName("experiment-overview-grid");
    grid.add(detailBlock("Experimental groups", groups.stream()
        .map(group -> group.name() + " — " + samplesPerGroup.getOrDefault(group.id(), 0L)
            + " samples registered")
        .toList()));
    grid.add(detailBlock("Variables", variables.stream()
        .map(variable -> variable.name() + " (" + String.join(" / ", variable.levels()) + ")")
        .toList()));
    grid.add(detailBlock("Confounding variables", confounding.stream()
        .map(ConfoundingVariableInformation::variableName)
        .toList()));
    band.add(grid);
    return band;
  }

  /**
   * The ordered pair of steps that unlocks sample registration.
   *
   * <p>Rendered as a numbered list because it genuinely is a sequence: a group's condition is one
   * level per variable, so the variables have to exist before a condition can be chosen. Each step
   * states its own state and the second states what it unlocks, so a reader who only ever reads one
   * line still learns why the design is being asked for.</p>
   */
  private static Div designSteps(List<ExperimentalVariableInformation> variables,
      List<ExperimentalGroup> groups) {
    Div steps = new Div();
    steps.addClassName("experiment-overview-steps");
    steps.add(designStep(1, "Variables",
        variables.isEmpty()
            ? "None defined yet"
            : pluralise(variables.size(), "variable", "variables") + " defined",
        !variables.isEmpty()));
    steps.add(designStep(2, "Experimental groups",
        groups.isEmpty()
            ? "None defined yet — this is what unlocks sample registration"
            : pluralise(groups.size(), "group", "groups") + " defined",
        !groups.isEmpty()));
    return steps;
  }

  private static Div designStep(int ordinal, String label, String detail, boolean done) {
    Div step = new Div();
    step.addClassName("experiment-overview-step");
    Span marker = new Span(Integer.toString(ordinal));
    marker.addClassName("experiment-overview-step-marker");
    if (done) {
      // Done swaps the ordinal for a check, as the progress rail does. The glyph plus the state
      // class carry the meaning, so the colour is reinforcement rather than the signal.
      step.addClassName("experiment-overview-step--done");
      marker.removeAll();
      Icon check = VaadinIcon.CHECK.create();
      check.getElement().setAttribute("aria-hidden", "true");
      marker.add(check);
    }
    Span labelSpan = new Span(label);
    labelSpan.addClassName("experiment-overview-step-label");
    Span detailSpan = new Span(detail);
    detailSpan.addClassName("experiment-overview-step-detail");
    step.add(marker, labelSpan, detailSpan);
    return step;
  }

  private static String pluralise(int count, String singular, String plural) {
    return count + " " + (count == 1 ? singular : plural);
  }

  private void renderCards(Div cards, Data data) {
    cards.removeAll();
    cards.add(samplesCard(data.progress(), data.samples()));
    cards.add(datasetCard(data.progress(), data.measurements(), data.assays(),
        data.assayLoadFailed()));
  }

  /**
   * The experiment's samples, as the project overview renders them.
   *
   * <p>This card is now the shared {@link SnapshotPanel}, the same component the project overview
   * uses. It used to be a look-alike with its own anatomy, its own state words ("Registered", where
   * the shared vocabulary says Complete) and its own table layout, so two pages describing the same
   * samples at the same grain disagreed about how to describe them.</p>
   */
  private Div samplesCard(ExperimentProgress progress, List<RecentSample> samples) {
    int count = progress.sampleCount();
    return SnapshotPanel.samples(progress, samples,
        AppRoutes.ProjectRoutes.SAMPLES.formatted(projectIdValue(), experimentIdValue()),
        count > 0 ? "All " + count + (count == 1 ? " sample" : " samples") + " →" : "");
  }

  /**
   * The experiment's measurement datasets, one row per dataset.
   *
   * <p>Two corrections, both taken from the project overview (project owner, 2026-11-03):</p>
   *
   * <ul>
   *   <li><b>Dataset grain, not measurement grain.</b> The card previewed the three newest
   *       measurements with their primary-data status. But "not complete" is a condition of a
   *       dataset, and a list of measurement IDs cannot say which dataset is behind -- which is the
   *       question the card exists to answer. It now states one row per dataset, the way the
   *       project overview's panel does.</li>
   *   <li><b>No completeness ring.</b> The card carried the count, a ring and a gap sentence: three
   *       encodings of one fact, and the ring appeared on this card alone, so the two cards had
   *       visibly different weight. The ratio is now stated once, in words. A proportion is the
   *       ring's encoding, and "17 of 20" is more precise than an arc.</li>
   * </ul>
   *
   * <p>The state tag is gone with them: the table's "Data availability" column states every
   * dataset's state on its own row, which is the same fact at a stronger grain.</p>
   */
  private Div datasetCard(ExperimentProgress progress, List<RecentMeasurement> measurements,
      List<AssayStanding> assays, boolean assayLoadFailed) {
    int datasets = assays.size();
    return SnapshotPanel.datasets(progress, measurements, assays, assayLoadFailed,
        AppRoutes.ProjectRoutes.MEASUREMENT_DATASET.formatted(projectIdValue(), experimentIdValue()),
        datasets == 0 ? "" : "All " + datasets + (datasets == 1 ? " dataset" : " datasets")
            + " →");
  }

  private void navigateToDesign() {
    getUI().ifPresent(ui -> ui.navigate(ExperimentDesignMain.class, routeParameters()));
  }

  private void navigateToSetup() {
    getUI().ifPresent(ui -> ui.navigate(ExperimentSetupMain.class, routeParameters()));
  }

  private RouteParameters routeParameters() {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, context.projectId().orElseThrow().value()),
        new RouteParam(EXPERIMENT_ID_PARAMETER, context.experimentId().orElseThrow().value()));
  }

  /** The project of the loaded context, as the route's identifier: the cards' links need it. */
  private String projectIdValue() {
    return context.projectId().orElseThrow().value();
  }

  /** The experiment of the loaded context, as the route's identifier. */
  private String experimentIdValue() {
    return context.experimentId().orElseThrow().value();
  }

  private static Div detailBlock(String title, List<String> entries) {
    Div block = new Div();
    block.addClassName("experiment-overview-detail");
    Span heading = new Span(title);
    heading.addClassName("experiment-overview-detail-title");
    block.add(heading);
    if (entries.isEmpty()) {
      block.add(note("None yet"));
      return block;
    }
    entries.forEach(entry -> {
      Span item = new Span(entry);
      item.addClassName("experiment-overview-detail-item");
      block.add(item);
    });
    return block;
  }

  private static Div actions(Component... components) {
    Div actions = new Div();
    actions.addClassName("experiment-overview-actions");
    actions.add(components);
    return actions;
  }

  private static Button action(String label, Runnable onClick) {
    Button button = new Button(label);
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(event -> onClick.run());
    return button;
  }

  /**
   * The page header: the view name as the H1, the experiment as the line beneath it.
   *
   * <p>The H1 still names the <em>view</em>, not the entity, so this page follows the same rule as
   * its sibling tabs ("View Samples", "View Measurements", "Download Raw Data") and the project
   * settings page. The experiment name rides underneath as a subtitle because the breadcrumb cannot
   * carry it reliably: crumbs truncate at 42ch, so a long name arrives as "… how the …" and the one
   * place the user could read the experiment in full is this page. That is also why the name is not
   * repeated in the Setup band's grid any more -- a third copy, one screen below, would be noise.</p>
   *
   * <p>Package-private so a spec can assert the header without a {@code UI}: {@link #reload()}
   * returns early when there is no current UI.</p>
   */
  Div pageHeader(Experiment experiment) {
    Div header = new Div();
    header.addClassName("experiment-overview-header");
    Span viewName = new Span("Experiment overview");
    viewName.addClassName("experiment-overview-title");
    header.add(viewName);
    // A blank name is a real state (the name is required, so it means the experiment was never
    // fully set up), and an empty subtitle would just look like a rendering fault.
    nameEntries(experiment).forEach(name -> {
      Paragraph subtitle = new Paragraph(name);
      subtitle.addClassName("experiment-overview-subtitle");
      header.add(subtitle);
    });
    return header;
  }

  private static Span sectionTitle(String text) {
    Span title = new Span(text);
    title.addClassName("experiment-overview-band-title");
    return title;
  }

  private static Span stateTag(String text, String tone) {
    Span tag = new Span();
    tag.addClassNames("experiment-overview-tag", "experiment-overview-tag--" + tone);
    if ("locked".equals(tone)) {
      Icon lock = VaadinIcon.LOCK.create();
      lock.addClassName("experiment-overview-tag-icon");
      tag.add(lock);
    }
    tag.add(new Span(text));
    return tag;
  }

  private static Span note(String text) {
    Span note = new Span(text);
    note.addClassName("experiment-overview-note");
    return note;
  }

  private static Span hint(String text) {
    Span hint = new Span(text);
    hint.addClassName("experiment-overview-hint");
    return hint;
  }

  private static Span message(String text) {
    Span message = new Span(text);
    message.addClassName("experiment-overview-message");
    return message;
  }

  /** The experiment's datasets, plus whether the query failed so the card can say so. */
  private record AssayLoad(List<AssayStanding> assays, boolean failed) {

  }

  private record Data(ExperimentProgress progress, List<RecentSample> samples,
                      List<RecentMeasurement> measurements, AssayLoad assayLoad) {

    List<AssayStanding> assays() {
      return assayLoad.assays();
    }

    boolean assayLoadFailed() {
      return assayLoad.failed();
    }
  }
}
