package life.qbic.projectmanagement.domain.model.measurement.event;

import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.measurement.MeasurementId;

/**
 * Indicates that a measurement has been created.
 *
 * @since 1.0.0
 */
public class MeasurementCreatedEvent extends DomainEvent {

  private final MeasurementId measurementId;

  public MeasurementCreatedEvent(MeasurementId measurementId) {
    this.measurementId = Objects.requireNonNull(measurementId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public MeasurementId measurementId() {
    return this.measurementId;
  }

}
