package life.qbic.projectmanagement.domain.model.measurement.event;


import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChangedEvent;

/**
 * <b>Measurement Update Event</b>
 * <p>
 * An event that indicates that one or more measurements were updated
 *
 * @since 1.0.0
 */
public class MeasurementsUpdated extends ProjectChangedEvent {

  @Serial
  private static final long serialVersionUID = 365535784754819361L;

  private MeasurementsUpdated(ProjectId projectId) {
    super(projectId);
  }

  public static MeasurementsUpdated create(ProjectId projectId) {
    return new MeasurementsUpdated(projectId);
  }

  @JsonGetter("occurredOn")
  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

}
