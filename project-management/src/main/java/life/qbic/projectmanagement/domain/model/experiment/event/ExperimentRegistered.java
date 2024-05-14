package life.qbic.projectmanagement.domain.model.experiment.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Experiment Registration Event</b>
 * <p>
 * An event that indicates that an experiment was registered
 *
 * @since 1.0.0
 */
public class ExperimentRegistered extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365584674754819361L;

  private ExperimentRegistered(ProjectId projectId) {
    super(projectId);
  }

  public static ExperimentRegistered create(ProjectId projectId) {
    return new ExperimentRegistered(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

}
