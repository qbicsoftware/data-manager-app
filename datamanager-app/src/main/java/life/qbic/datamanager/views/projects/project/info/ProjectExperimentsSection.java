package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.List;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.ExperimentProgress;

/**
 * Experiments section of the project page: one card per experiment, carrying its sample, measurement
 * and primary-data counts, plus the {@code Add experiment} action and a way into the experiment.
 *
 * <p>This replaces the former name-only experiments rail in the right column. The counts come from
 * the {@code experiment_progress} read model through {@link ProjectQueryService}, so the section and
 * the progress section above it cannot disagree about the same numbers.</p>
 *
 * <p>Inline per-experiment record previews (last three registered/modified) are a planned follow-up;
 * they need additional query methods and are deliberately not part of this increment.</p>
 *
 * @since 1.19.0
 */
public class ProjectExperimentsSection extends Div {

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

  public void setProjectId(String projectId) {
    this.projectId = requireNonNull(projectId, "projectId must not be null");
    reload();
  }

  /**
   * Registers a listener for the request to create a new experiment. The section owns the action;
   * the owning page owns the creation workflow.
   */
  public Registration addAddExperimentListener(
      ComponentEventListener<AddExperimentRequestedEvent> listener) {
    return addListener(AddExperimentRequestedEvent.class, listener);
  }

  private void reload() {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    content.removeAll();
    content.add(message("Loading experiments …"));
    ReactiveUiBridge.onUi(ui,
        projectQueryService.getExperimentProgress(projectId).collectList(),
        this::render,
        error -> {
          content.removeAll();
          content.add(message("Experiments could not be loaded."));
        });
  }

  private void render(List<ExperimentProgress> experiments) {
    content.removeAll();
    Div header = new Div(heading("Experiments"));
    header.addClassName("project-experiments-header");
    content.add(header);

    if (experiments.isEmpty()) {
      content.add(onboarding());
      return;
    }
    Div cards = new Div();
    cards.addClassName("project-experiments-cards");
    experiments.forEach(experiment -> cards.add(experimentCard(experiment)));
    content.add(cards);
  }

  /**
   * Onboarding hero shown when the project has no experiment yet. The action is the same one the
   * identity band raises, so the empty state and the band cannot diverge.
   */
  private Div onboarding() {
    Div hero = new Div();
    hero.addClassName("project-experiments-onboarding");
    Span title = new Span("Create your first experiment");
    title.addClassName("project-experiments-onboarding-title");
    Span text = new Span("Experiments are where samples, measurements and primary data live — "
        + "all of it starts with one experiment.");
    text.addClassName("project-experiments-onboarding-text");
    hero.add(title, text, addExperimentButton());
    return hero;
  }

  private Div experimentCard(ExperimentProgress experiment) {
    Div card = new Div();
    card.addClassName("project-experiment-card");

    Span name = new Span(experiment.experimentName());
    name.addClassName("project-experiment-card-name");
    card.add(name);

    Div stats = new Div();
    stats.addClassName("project-experiment-card-stats");
    stats.add(stat("Samples", experiment.sampleCount()));
    stats.add(stat("Measurements", experiment.measurementCount()));
    stats.add(stat("Without primary data", experiment.measurementsWithoutPrimaryData()));
    card.add(stats);

    RouterLink open = new RouterLink("Open experiment", ExperimentInformationMain.class,
        new RouteParameters(
            new RouteParam(PROJECT_ID_PARAMETER, projectId),
            new RouteParam(EXPERIMENT_ID_PARAMETER, experiment.experimentId())));
    open.addClassName("project-experiment-card-open");
    card.add(open);
    return card;
  }

  private Button addExperimentButton() {
    Button add = new Button("Add experiment");
    add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    add.addClassName("project-experiments-add");
    add.addClickListener(click -> fireEvent(new AddExperimentRequestedEvent(this,
        click.isFromClient())));
    return add;
  }

  private static Div stat(String label, int value) {
    Div fact = new Div();
    fact.addClassName("project-experiment-card-stat");
    Span number = new Span(Integer.toString(value));
    number.addClassName("project-experiment-card-stat-value");
    Span caption = new Span(label);
    caption.addClassName("project-experiment-card-stat-label");
    fact.add(number, caption);
    return fact;
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
