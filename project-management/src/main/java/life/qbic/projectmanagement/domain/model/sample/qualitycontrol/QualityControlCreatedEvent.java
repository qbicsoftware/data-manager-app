package life.qbic.projectmanagement.domain.model.sample.qualitycontrol;

import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;

/**
 * Indicates that a QC object has been created, deleted or changed.
 *
 * @since 1.0.0
 */
public class QualityControlCreatedEvent extends DomainEvent {

  private final Long qualityControlID;

  public QualityControlCreatedEvent(Long qcID) {
    this.qualityControlID = Objects.requireNonNull(qcID);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public Long qualityControlID() {
    return this.qualityControlID;
  }

}
