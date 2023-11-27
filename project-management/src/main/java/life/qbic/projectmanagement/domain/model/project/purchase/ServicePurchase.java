package life.qbic.projectmanagement.domain.model.project.purchase;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
public class ServicePurchase {

  private ProjectId projectId;

  private Instant purchasedOn;

  @OneToOne
  @JoinColumn(name = "offer_reference", referencedColumnName = "id")
  private Offer offer;

  @Id
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
