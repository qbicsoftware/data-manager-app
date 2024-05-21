package life.qbic.projectmanagement.domain.model.measurement.event;

import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;

/**
 * Indicates that a certain measurement's metadata has been updated.
 *
 * @since 1.0.0
 */
public class MeasurementUpdatedEvent extends DomainEvent {

  private final Instant occurredOn;
  private final MeasurementId measurementId;

  public MeasurementUpdatedEvent(MeasurementId measurementId) {
    this.measurementId = Objects.requireNonNull(measurementId);
    this.occurredOn = Instant.now();
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public MeasurementId measurementId() {
    return this.measurementId;
  }

}
