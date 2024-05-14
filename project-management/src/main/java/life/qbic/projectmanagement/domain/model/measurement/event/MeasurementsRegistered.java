package life.qbic.projectmanagement.domain.model.measurement.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Measurement Registration Event</b>
 * <p>
 * An event that indicates that one or more measurements were registered
 *
 * @since 1.0.0
 */
public class MeasurementsRegistered extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365535784754819361L;

  private MeasurementsRegistered(ProjectId projectId) {
    super(projectId);
  }

  public static MeasurementsRegistered create(ProjectId projectId) {
    return new MeasurementsRegistered(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

}
