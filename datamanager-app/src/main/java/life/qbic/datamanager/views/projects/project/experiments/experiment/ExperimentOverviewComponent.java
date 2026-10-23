package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.requireNonNull;

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
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.CompletenessRing;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentDesignMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentSetupMain;
import life.qbic.datamanager.views.projects.project.measurements.MeasurementMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentalVariableInformation;
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

    Mono<Data> data = Mono.zip(
            projectQueryService.getExperimentProgress(projectId, experimentId.value()),
            projectQueryService.getRecentSamples(projectId, experimentId.value()).collectList(),
            projectQueryService.getRecentMeasurements(projectId, experimentId.value()).collectList())
        .map(values -> new Data(values.getT1(), values.getT2(), values.getT3()));
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
    cards.add(datasetCard(data.progress(), data.measurements()));
  }

  private Div samplesCard(ExperimentProgress progress, List<RecentSample> samples) {
    boolean started = progress.sampleCount() > 0;
    String status = samples.isEmpty()
        ? "Nothing registered yet"
        : "Registered · " + Utility.relativeTime(samples.getFirst().changed());
    List<List<String>> rows = samples.stream()
        .map(sample -> List.of(sample.sampleCode(), sample.sampleName(),
            Utility.relativeTime(sample.changed())))
        .toList();
    return snapshotCard("Samples", "samples", progress.sampleCount(),
        started ? "Registered" : "Not started", started ? "done" : "active", note(status), null,
        new String[]{"Sample ID", "Sample name", "Changed"}, rows, "No samples to show yet",
        () -> navigate(SampleInformationMain.class));
  }

  private Div datasetCard(ExperimentProgress progress, List<RecentMeasurement> measurements) {
    int total = progress.measurementCount();
    int missing = progress.measurementsWithoutPrimaryData();
    boolean hasSamples = progress.sampleCount() > 0;
    String state;
    String tone;
    if (!hasSamples) {
      state = "Locked";
      tone = "locked";
    } else if (total == 0) {
      state = "Ready to register";
      tone = "active";
    } else if (missing > 0) {
      state = "Not complete yet";
      tone = "incomplete";
    } else {
      state = "Complete";
      tone = "done";
    }
    List<List<String>> rows = measurements.stream()
        .map(measurement -> List.of(measurement.measurementCode(),
            primaryDataLabel(measurement), Utility.relativeTime(measurement.changed())))
        .toList();
    return snapshotCard("Dataset", "measurements", total, state, tone,
        note(gapSentence(progress)), completenessRing(progress),
        new String[]{"Measurement ID", "Primary data", "Changed"}, rows,
        "No measurements to show yet", () -> navigate(MeasurementMain.class));
  }

  /**
   * The Dataset card's completeness ring, placed in the card's top-right corner.
   *
   * <p>The ring shows the coverage proportion; the status sentence beside the metric states the
   * outstanding count, never the ratio, so the reader is not asked to do the subtraction the ring
   * already did. The ring never carries the meaning alone — it also exposes
   * {@code role=progressbar} with an {@code aria-valuetext}.</p>
   */
  private Component completenessRing(ExperimentProgress progress) {
    int total = progress.measurementCount();
    int withData = progress.measurementsWithPrimaryData();
    int missing = progress.measurementsWithoutPrimaryData();
    String aria = total == 0
        ? "No measurements registered yet"
        : missing > 0
            ? missing + " of " + total + " measurements still need primary data"
            : "All " + total + " measurements have primary data";
    return new CompletenessRing(withData, total, aria);
  }

  /** The one sentence that states the gap, never the ratio. */
  private static String gapSentence(ExperimentProgress progress) {
    int total = progress.measurementCount();
    int missing = progress.measurementsWithoutPrimaryData();
    if (total == 0) {
      return "No measurements registered yet";
    }
    return missing > 0
        ? missing + (missing == 1
            ? " measurement still needs primary data"
            : " measurements still need primary data")
        : "All " + total + " have primary data";
  }

  private Div snapshotCard(String label, String unit, int count, String state, String tone,
      Component status, Component accessory, String[] columns, List<List<String>> rows,
      String emptyRow, Runnable open) {
    Div panel = new Div();
    panel.addClassName("experiment-overview-card");

    Div head = new Div();
    head.addClassName("experiment-overview-card-head");
    Span labelSpan = new Span(label);
    labelSpan.addClassName("experiment-overview-card-label");
    head.add(surfaceIcon(label), labelSpan,
        stateTag(state, tone), hint("Open " + label.toLowerCase()));
    if (accessory != null) {
      accessory.addClassName("experiment-overview-card-accessory");
      head.add(accessory);
    }
    panel.add(head);

    Div metric = new Div();
    metric.addClassName("experiment-overview-card-metric");
    Span value = new Span(Integer.toString(count));
    value.addClassName("experiment-overview-card-value");
    Span unitSpan = new Span(unit);
    unitSpan.addClassName("experiment-overview-card-unit");
    metric.add(value, unitSpan);
    panel.add(metric);
    panel.add(status);

    Div list = new Div();
    list.addClassName("experiment-overview-card-rows");
    list.add(row(columns, true));
    if (rows.isEmpty()) {
      list.add(placeholderRow(emptyRow));
    } else {
      rows.forEach(cells -> list.add(row(cells.toArray(new String[0]), false)));
    }
    panel.add(list);

    Utility.activateOnClickAndKeys(panel, "button", open);
    return panel;
  }

  private void navigateToDesign() {
    getUI().ifPresent(ui -> ui.navigate(ExperimentDesignMain.class, routeParameters()));
  }

  private void navigateToSetup() {
    getUI().ifPresent(ui -> ui.navigate(ExperimentSetupMain.class, routeParameters()));
  }

  private void navigate(Class<? extends Component> target) {
    getUI().ifPresent(ui -> ui.navigate(target, routeParameters()));
  }

  private RouteParameters routeParameters() {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, context.projectId().orElseThrow().value()),
        new RouteParam(EXPERIMENT_ID_PARAMETER, context.experimentId().orElseThrow().value()));
  }

  private static String primaryDataLabel(RecentMeasurement measurement) {
    if (!measurement.hasPrimaryData()) {
      return "Not attached";
    }
    return measurement.primaryDataFiles() + " files · " + Utility.humanReadableSize(
        measurement.primaryDataBytes() == null ? 0L : measurement.primaryDataBytes());
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

  private static Icon surfaceIcon(String label) {
    Icon glyph = switch (label) {
      case "Samples" -> VaadinIcon.FLASK.create();
      case "Dataset" -> VaadinIcon.DATABASE.create();
      default -> VaadinIcon.RECORDS.create();
    };
    glyph.addClassName("experiment-overview-card-icon");
    glyph.getElement().setAttribute("aria-hidden", "true");
    return glyph;
  }

  private static Div row(String[] cells, boolean header) {
    Div row = new Div();
    row.addClassName("experiment-overview-card-row");
    for (int index = 0; index < cells.length; index++) {
      Span span = new Span(cells[index]);
      if (header) {
        span.addClassName("experiment-overview-card-cell-header");
      } else if (index == 0) {
        span.addClassName("identifier");
      }
      row.add(span);
    }
    return row;
  }

  private static Div placeholderRow(String text) {
    Div row = new Div();
    row.addClassName("experiment-overview-card-row");
    row.add(new Span(text));
    return row;
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

  private record Data(ExperimentProgress progress, List<RecentSample> samples,
                      List<RecentMeasurement> measurements) {

  }
}
