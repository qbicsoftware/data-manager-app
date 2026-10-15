package life.qbic.datamanager.views.projects.project.info;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.util.List;
import life.qbic.datamanager.views.general.ReactiveUiBridge;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.ExperimentProgress;

/**
 * Read-only progress section of the project page, driven by {@link ProjectQueryService}.
 *
 * <p>States where the project stands — how many experiments, samples and measurements it has, and
 * how many measurements still lack primary data — and names the experiments that need attention. It
 * is the first consumer of the project-page read models (the {@code experiment_progress} view,
 * exposed through {@link ProjectQueryService}).</p>
 *
 * <p>This is the incremental first section of the project-page rework: the surrounding page is left
 * unchanged for now.</p>
 *
 * @since 1.19.0
 */
public class ProjectProgressSection extends Div {

  private final transient ProjectQueryService projectQueryService;
  private final Div content = new Div();

  public ProjectProgressSection(ProjectQueryService projectQueryService) {
    this.projectQueryService = requireNonNull(projectQueryService,
        "projectQueryService must not be null");
    addClassName("project-progress-section");
    content.addClassName("project-progress-content");
    add(content);
  }

  /**
   * Loads and renders the progress for the given project.
   *
   * @param projectId the project identifier
   */
  public void setProjectId(String projectId) {
    requireNonNull(projectId, "projectId must not be null");
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    content.removeAll();
    content.add(message("Loading project status …"));
    ReactiveUiBridge.onUi(ui,
        projectQueryService.getExperimentProgress(projectId).collectList(),
        this::render,
        error -> {
          content.removeAll();
          content.add(message("Project status could not be loaded."));
        });
  }

  private void render(List<ExperimentProgress> progress) {
    content.removeAll();
    content.add(heading("Where the project stands"));

    Div stages = new Div();
    stages.addClassName("project-progress-stages");
    stages.add(stage("Experiments", progress.size()));
    stages.add(stage("Samples",
        progress.stream().mapToInt(ExperimentProgress::sampleCount).sum()));
    stages.add(stage("Measurements",
        progress.stream().mapToInt(ExperimentProgress::measurementCount).sum()));
    stages.add(stage("Without primary data",
        progress.stream().mapToInt(ExperimentProgress::measurementsWithoutPrimaryData).sum()));
    content.add(stages);

    List<ExperimentProgress> attention = progress.stream()
        .filter(entry -> entry.measurementsWithoutPrimaryData() > 0)
        .toList();
    if (!attention.isEmpty()) {
      content.add(attentionBlock(attention));
    }
  }

  private static Div attentionBlock(List<ExperimentProgress> attention) {
    Div block = new Div();
    block.addClassName("project-progress-attention");
    Span title = new Span(attention.size() == 1
        ? "1 experiment needs attention"
        : attention.size() + " experiments need attention");
    title.addClassName("project-progress-attention-title");
    block.add(title);
    attention.forEach(entry -> {
      Div row = new Div(new Span(entry.experimentName() + " · "
          + entry.measurementsWithoutPrimaryData() + " of " + entry.measurementCount()
          + " measurements have no primary data"));
      row.addClassName("project-progress-attention-row");
      block.add(row);
    });
    return block;
  }

  private static Div stage(String label, int value) {
    Div card = new Div();
    card.addClassName("project-progress-stage");
    Span number = new Span(Integer.toString(value));
    number.addClassName("project-progress-stage-value");
    Span caption = new Span(label);
    caption.addClassName("project-progress-stage-label");
    card.add(number, caption);
    return card;
  }

  private static Span heading(String text) {
    Span heading = new Span(text);
    heading.addClassName("project-progress-heading");
    return heading;
  }

  private static Span message(String text) {
    Span message = new Span(text);
    message.addClassName("project-progress-message");
    return message;
  }
}
