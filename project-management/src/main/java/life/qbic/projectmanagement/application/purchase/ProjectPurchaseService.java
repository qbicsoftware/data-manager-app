package life.qbic.projectmanagement.application.purchase;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.ProjectPurchaseStorage;
import life.qbic.projectmanagement.application.api.PurchaseStoreException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged;
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
    addPurchases(projectId, List.of(offer));
  }

  public void addPurchases(String projectId, List<OfferDTO> offers) {
    var projectReference = ProjectId.parse(projectId);
    var purchaseDate = Instant.now();
    List<ServicePurchase> servicePurchases = offers.stream()
        .map(it -> Offer.create(it.signed(), it.fileName(), it.content()))
        .map(it -> ServicePurchase.create(projectReference, purchaseDate, it))
        .toList();
    try {
      storage.storePurchases(servicePurchases);
      dispatchSuccessfulPurchaseUpdate(projectReference);
    } catch (PurchaseStoreException e) {
      throw ApplicationException.wrapping(e);
    }
  }

  private void dispatchSuccessfulPurchaseUpdate(ProjectId projectReference) {
    ProjectChanged projectChanged = ProjectChanged.create(projectReference);
    DomainEventDispatcher.instance().dispatch(projectChanged);
  }

  /**
   * Lists all offers linked to a project
   *
   * @param projectId the projectId for which to search offers for
   * @return a list of all linked offers, can be empty, never null.
   */
  public List<Offer> linkedOffers(String projectId) {
    ProjectId parsedId = ProjectId.parse(projectId);
    return requireNonNull(storage.findOffersForProject(parsedId),
        "result must not be null");
  }

  public void deleteOffer(String projectId, long offerId) {
    storage.deleteOffer(projectId, offerId);
    dispatchSuccessfulPurchaseUpdate(ProjectId.parse(projectId));
  }

  public Optional<Offer> getOfferWithContent(String projectId, Long offerId) {
    return storage.findOfferForProject(projectId, offerId);
  }

}
