package life.qbic.projectmanagement.domain.model.project.purchase;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Service Purchase</b>
 *
 * <p>A cohesive collection of service items a customer has ordered for a project.</p>
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "service_purchase")
public class ServicePurchase {

  private ProjectId projectId;

  private Instant purchasedOn;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "offerReference", referencedColumnName = "id")
  private Offer offer;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected ServicePurchase() {

  }

  protected ServicePurchase(ProjectId projectId, Instant purchasedOn,
      Offer offer) {
    this.projectId = projectId;
    this.purchasedOn = purchasedOn;
    this.offer = offer;
    emitCreatedEvent();
  }

  public static ServicePurchase create(ProjectId projectId, Instant purchasedOn, Offer offer) {
    return new ServicePurchase(projectId, purchasedOn, offer);
  }

  public ProjectId project() {
    return this.projectId;
  }

  public Offer getOffer() {
    return offer;
  }

  public Instant purchasedOn() {
    return purchasedOn;
  }

  private void emitCreatedEvent() {
    //FIXME id is null
    //var createdEvent = new PurchaseCreatedEvent(this.id);
    //LocalDomainEventDispatcher.instance().dispatch(createdEvent);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServicePurchase that = (ServicePurchase) o;
    return Objects.equals(projectId, that.projectId) && Objects.equals(
        purchasedOn, that.purchasedOn) && Objects.equals(offer, that.offer)
        && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, purchasedOn, offer, id);
  }

  public Long getId() {
    return id;
  }
}
