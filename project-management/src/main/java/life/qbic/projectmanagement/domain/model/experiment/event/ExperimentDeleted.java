package life.qbic.projectmanagement.domain.model.experiment.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Experiment Deletion Event</b>
 * <p>
 * An event that indicates that an experiment was deleted
 *
 * @since 1.0.0
 */
public class ExperimentDeleted extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365584674754819361L;

  private ExperimentDeleted(ProjectId projectId) {
    super(projectId);
  }

  public static ExperimentDeleted create(ProjectId projectId) {
    return new ExperimentDeleted(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

}
