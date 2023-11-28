package life.qbic.projectmanagement.application.purchase;

import java.time.Instant;
import life.qbic.projectmanagement.application.api.ProjectPurchaseStorage;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.purchase.Offer;
import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;
import org.springframework.stereotype.Service;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class ProjectPurchaseService {

  private final ProjectPurchaseStorage storage;

  public ProjectPurchaseService(ProjectPurchaseStorage storage) {
    this.storage = storage;
  }

  public void addPurchase(String projectId, OfferDTO offer) {
    var theOffer = Offer.create( offer.signed(), offer.fileName(),
        offer.content());
    var projectReference = ProjectId.parse(projectId);
    var purchaseDate = Instant.now();

    ServicePurchase purchase = ServicePurchase.create(projectReference, purchaseDate, theOffer);

    storage.storePurchase(purchase);
  }
}
