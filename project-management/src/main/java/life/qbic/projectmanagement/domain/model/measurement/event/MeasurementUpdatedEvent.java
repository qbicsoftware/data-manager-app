package life.qbic.projectmanagement.domain.model.measurement.event;

import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;

/**
 * Indicates that a certain measurement's metadata has been updated.
 *
 * @since 1.0.0
 */
public class MeasurementUpdatedEvent extends DomainEvent {

  private final String projectId;
  private final String measurementId;

  public MeasurementUpdatedEvent(String projectId, String measurementId) {
    this.measurementId = Objects.requireNonNull(measurementId);
    this.projectId = Objects.requireNonNull(projectId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public String projectId() {
    return projectId;
  }

  public String measurementId() {
    return this.measurementId;
  }

}
