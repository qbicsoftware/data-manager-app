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
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity
@Table(name = "service_purchase")
public class ServicePurchase {

  private ProjectId projectId;

  private Instant purchasedOn;

  @OneToOne(cascade= CascadeType.ALL )
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
  }

  public static ServicePurchase create(ProjectId projectId, Instant purchasedOn, Offer offer) {
    return new ServicePurchase(projectId, purchasedOn, offer);
  }

  public ProjectId project() {
    return this.projectId;
  }
}
