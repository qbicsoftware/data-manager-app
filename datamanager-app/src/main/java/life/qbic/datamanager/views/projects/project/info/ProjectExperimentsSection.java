package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.logging.api.Logger;
import life.qbic.datamanager.views.MeasurementType;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.general.snapshot.SnapshotPanel;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.CreateExperimentMain;
import life.qbic.datamanager.views.projects.project.dataset.MeasurementDatasetMain;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.AssayStanding;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.application.query.RecentMeasurement;
import life.qbic.projectmanagement.application.query.RecentSample;
import reactor.core.publisher.Mono;

/**
 * Experiments section of the project page — the prototype's experiment entries.
 *
 * <p>Each entry is {@code name + domain chips + not-complete}, followed by two snapshot panels: the
 * newest samples and the newest measurements (with their primary-data status). The panels are the
 * way into the full views and state the same numbers the progress section does, so the entry does
 * not repeat them as separate counts.</p>
 *
 * <p>Snapshot rows come from the {@code experiment_recent_sample} / {@code experiment_recent_measurement}
 * read models through {@link ProjectQueryService}.</p>
 *
 * @since 1.19.0
 */
public class ProjectExperimentsSection extends Div {

  private static final Logger log = logger(ProjectExperimentsSection.class);

  private static final String PROJECT_ID_PARAMETER = "projectId";
  private static final String EXPERIMENT_ID_PARAMETER = "experimentId";

  private final transient ProjectQueryService projectQueryService;
  private final Div content = new Div();
  private String projectId;

  public ProjectExperimentsSection(ProjectQueryService projectQueryService) {
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
    addClassName("project-experiments-section");
    content.addClassName("project-experiments-content");
    add(content);
  }

  public void setProjectId(String projectId, Mono<List<ExperimentProgress>> progress) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    reload(requireNonNull(progress, "progress must not be null"));
  }

  /**
   * Registers a listener for the request to create a new experiment. The section owns the action;
   * the owning page owns the creation workflow.
   */
  public Registration addAddExperimentListener(
      ComponentEventListener<AddExperimentRequestedEvent> listener) {
    return addListener(AddExperimentRequestedEvent.class, listener);
  }

  private void reload(Mono<List<ExperimentProgress>> progress) {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    content.removeAll();
    content.add(message("Loading experiments …"));
    // `progress` is the page's single shared stream (cached by the parent); the recent rows are
    // specific to this section.
    Mono<Snapshot> snapshot = Mono.zip(
            progress,
            projectQueryService.getRecentSamples(projectId).collectList(),
            projectQueryService.getRecentMeasurements(projectId).collectList(),
            // The dataset-grain read model: one row per (experiment, analysis method). This is what
            // the dataset panel needs -- the progress model only knows omics domains.
            // A failed assay query must not take the experiments list down with it, but it must not
            // be silently reported as "no datasets" either -- that is indistinguishable from a
            // project that genuinely has none, and it hid a broken query once already. The failure is
            // logged at error level with the cause, and the panel says so (see Snapshot.assaysFailed).
            projectQueryService.getAssayStanding(projectId).collectList()
                .map(assays -> new AssayLoad(assays, false))
                .onErrorResume(error -> {
                  log.error("Assay standing could not be loaded for project " + projectId + ": "
                      + error.getClass().getSimpleName() + " (" + error.getMessage() + ")", error);
                  return Mono.just(new AssayLoad(List.of(), true));
                }))
        .map(values -> new Snapshot(values.getT1(), values.getT2(), values.getT3(), values.getT4()));
    ReactiveUiBridge.onUi(ui, snapshot, this::render, error -> {
      content.removeAll();
      content.add(message("Experiments could not be loaded."));
    });
  }

  private void render(Snapshot snapshot) {
    content.removeAll();
    Div header = new Div(heading("Experiments"));
    header.addClassName("project-experiments-header");
    // Offer creation where the experiments live, but only once there is a list to add to: with no
    // experiments the progress band above carries the call to action and a second button here
    // would repeat the same instruction in the same viewport.
    if (!snapshot.progress().isEmpty()) {
      header.add(createExperimentButton());
    }    content.add(header);

    if (snapshot.progress().isEmpty()) {
      // No onboarding hero: the progress band above already names the absence and carries the
      // call to action, and a second copy of the same button below it only split the attention.
      content.add(emptyExperiments());
      return;
    }
    Map<String, List<RecentSample>> samplesByExperiment = snapshot.samples().stream()
        .collect(Collectors.groupingBy(RecentSample::experimentId));
    Map<String, List<RecentMeasurement>> measurementsByExperiment = snapshot.measurements().stream()
        .collect(Collectors.groupingBy(RecentMeasurement::experimentId));
    Map<String, List<AssayStanding>> assaysByExperiment = snapshot.assayLoad().assays().stream()
        .collect(Collectors.groupingBy(AssayStanding::experimentId));

    Div cards = new Div();
    cards.addClassName("project-experiments-cards");
    for (ExperimentProgress experiment : snapshot.progress()) {
      cards.add(experimentCard(experiment,
          samplesByExperiment.getOrDefault(experiment.experimentId(), List.of()),
          measurementsByExperiment.getOrDefault(experiment.experimentId(), List.of()),
          assaysByExperiment.getOrDefault(experiment.experimentId(), List.of()),
          snapshot.assayLoad().failed()));
    }
    content.add(cards);
  }

  private Div experimentCard(ExperimentProgress experiment, List<RecentSample> samples,
      List<RecentMeasurement> measurements, List<AssayStanding> assays, boolean assaysFailed) {
    Div card = new Div();
    card.addClassName("project-experiment-card");

    Div identity = new Div();
    identity.addClassName("project-exp-identity");
    // The experiment's identity is the way into its overview; the snapshot panels open their own
    // surfaces (project owner, 2026-10-19).
    Anchor name = Utility.textLink(experiment.experimentName(),
        AppRoutes.ProjectRoutes.EXPERIMENT.formatted(projectId, experiment.experimentId()),
        "project-exp-name");
    Div facets = new Div();
    facets.addClassName("project-exp-facets");
    domains(experiment).forEach(domain -> facets.add(chip(domain, "project-exp-chip--domain")));
    // No status chip. Every state it could report -- "Not started" and "Not complete yet" -- is
    // already tagged on the panel that owns it, one line below, beside the count and the route
    // into that surface. Stating it here too said the same thing twice in one band, and said it
    // more weakly: nothing here named where the work actually is.
    identity.add(name, facets);
    card.add(identity);

    Div panels = new Div();
    panels.addClassName("project-exp-snapshot");
    // Both panels sit in a wrapping flex row, so each has to claim an equal share of it: without
    // the basis a panel collapses to its content width and leaves the rest of the row empty.
    panels.add(samplesPanel(experiment, samples).equalInRow());
    panels.add(datasetPanel(experiment, measurements, assays, assaysFailed).equalInRow());
    card.add(panels);
    return card;
  }

  /**
   * The experiment's newest samples.
   *
   * <p>Delegates to the shared {@link SnapshotPanel} so this page and the experiment overview render
   * one anatomy from one place. The panel previews <em>recency</em>: the three rows are the newest,
   * "Changed" is the column that carries the meaning, and the header band offers the way to the full
   * list.</p>
   */
  private SnapshotPanel samplesPanel(ExperimentProgress experiment,
      List<RecentSample> samples) {
    return SnapshotPanel.samples(experiment, samples,
        AppRoutes.ProjectRoutes.SAMPLES.formatted(projectId, experiment.experimentId()),
        experiment.sampleCount() > 0 ? "All " + experiment.sampleCount()
            + (experiment.sampleCount() == 1 ? " sample" : " samples") + " \u2192" : "");
  }

  /**
   * The experiment's measurement datasets, one row per dataset.
   *
   * <p>Delegates to the shared {@link SnapshotPanel}: the dataset-grain table, the one-row-per-dataset
   * decision and the {@code DataState} vocabulary all live there now, so the two pages that show an
   * experiment's datasets cannot describe them differently. See the method's own javadoc for what a
   * row is, where the method comes from, and why there is no header tag.</p>
   */
  private SnapshotPanel datasetPanel(ExperimentProgress experiment,
      List<RecentMeasurement> measurements, List<AssayStanding> assays, boolean assaysFailed) {
    int datasets = assays.size();
    return SnapshotPanel.datasets(experiment, measurements, assays, assaysFailed,
        AppRoutes.ProjectRoutes.MEASUREMENT_DATASET.formatted(projectId, experiment.experimentId()),
        datasets == 0 ? "" : "All " + datasets + (datasets == 1 ? " dataset" : " datasets")
            + " \u2192");
  }

  private RouteParameters experimentRouteParameters(String experimentId) {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_PARAMETER, projectId),
        new RouteParam(EXPERIMENT_ID_PARAMETER, experimentId));
  }

  /**
   * The experiments list when there is nothing in it. It does not repeat the progress band's primary
   * button, but it is not dead text either: the sentence names the action it is asking for, so it
   * carries a link to the same route. A reader who has scrolled to the section and started reading
   * it should not have to scroll back up to act.
   */
  private Div emptyExperiments() {
    Div empty = new Div();
    empty.addClassName("project-experiments-empty");
    Anchor create = Utility.textLink("Create experiment",
        AppRoutes.ProjectRoutes.EXPERIMENT_CREATION.formatted(projectId),
        "project-experiments-empty-action");
    Span text = new Span("No experiments yet. ");
    text.addClassName("project-experiments-empty-text");
    empty.add(text, create);
    return empty;
  }

  private Button createExperimentButton() {
    Button create = new Button("Create experiment");
    create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    create.addClassName("project-experiments-add");
    create.addClickListener(click -> fireEvent(new AddExperimentRequestedEvent(this,
        click.isFromClient())));
    return create;
  }

  private static List<String> domains(ExperimentProgress experiment) {
    return experiment.assayTypes().stream()
        .map(type -> MeasurementType.from(type).getType())
        .toList();
  }

  private static Span chip(String text, String modifier) {
    Span chip = new Span(text);
    chip.addClassNames("project-exp-chip", modifier);
    return chip;
  }

  private static Span heading(String text) {
    Span heading = new Span(text);
    heading.addClassName("project-experiments-heading");
    return heading;
  }

  private static Span message(String text) {
    Span message = new Span(text);
    message.addClassName("project-experiments-message");
    return message;
  }

  /** The section's data: per-experiment progress plus the newest rows for the snapshot panels. */
  /** Assay rows plus whether the query failed, so the panel can say "could not load". */
  private record AssayLoad(List<AssayStanding> assays, boolean failed) {

  }

  private record Snapshot(List<ExperimentProgress> progress,
                              List<RecentSample> samples,
                              List<RecentMeasurement> measurements,
                              AssayLoad assayLoad) {

  }

  /** Fired when the user asks to create a new experiment. */
  public static class AddExperimentRequestedEvent extends
      ComponentEvent<ProjectExperimentsSection> {

    @Serial
    private static final long serialVersionUID = 1L;

    public AddExperimentRequestedEvent(ProjectExperimentsSection source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
