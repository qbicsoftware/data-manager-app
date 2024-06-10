package life.qbic.projectmanagement.domain.model.project.purchase;

import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;

/**
 * Indicates that a purchase/offer object has been created, deleted or changed.
 *
 * @since 1.0.0
 */
public class PurchaseCreatedEvent extends DomainEvent {

  private final Long purchaseID;

  public PurchaseCreatedEvent(Long purchaseID) {
    this.purchaseID = Objects.requireNonNull(purchaseID);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  public Long purchaseID() {
    return this.purchaseID;
  }

}
