package life.qbic.projectmanagement.domain.model.experiment.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Experiment Upate Event</b>
 * <p>
 * An event that indicates that an experiment was changed/updated
 *
 * @since 1.0.0
 */
public class ExperimentUpdated extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365584675894819361L;

  private ExperimentUpdated(ProjectId projectId) {
    super(projectId);
  }

  public static ExperimentUpdated create(ProjectId projectId) {
    return new ExperimentUpdated(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

}
