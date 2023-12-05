package life.qbic.projectmanagement.application.purchase;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.ProjectPurchaseStorage;
import life.qbic.projectmanagement.application.api.PurchaseStoreException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.purchase.Offer;
import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;
import org.springframework.stereotype.Service;

/**
 * <b>Project Purchase Service</b>
 * <p>
 * A service that enables actions on project purchase services.
 *
 * @since 1.0.0
 */
@Service
public class ProjectPurchaseService {

  private static final Logger log = logger(ProjectPurchaseService.class);
  private final ProjectPurchaseStorage storage;

  public ProjectPurchaseService(ProjectPurchaseStorage storage) {
    this.storage = storage;
  }

  /**
   * Adds an offer to a project.
   *
   * @param projectId the project the offer is related to
   * @param offer     the offer content
   * @since 1.0.0
   */
  public void addPurchase(String projectId, OfferDTO offer) {
    var theOffer = Offer.create(offer.signed(), offer.fileName(),
        offer.content());
    var projectReference = ProjectId.parse(projectId);
    var purchaseDate = Instant.now();

    ServicePurchase purchase = ServicePurchase.create(projectReference, purchaseDate, theOffer);

    try {
      storage.storePurchase(purchase);
    } catch (PurchaseStoreException e) {
      throw ApplicationException.wrapping(e);
    }
  }
}
