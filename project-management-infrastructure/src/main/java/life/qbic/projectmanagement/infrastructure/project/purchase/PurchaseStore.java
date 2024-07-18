package life.qbic.projectmanagement.infrastructure.project.purchase;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.api.ProjectPurchaseStorage;
import life.qbic.projectmanagement.application.api.PurchaseStoreException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.purchase.Offer;
import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Purchase Store</b>
 *
 * <p>Implementation of the {@link ProjectPurchaseStorage} interface.</p>
 *
 * @since 1.0.0
 */
@Component
public class PurchaseStore implements ProjectPurchaseStorage {

  private static final Logger log = logger(PurchaseStore.class);

  private final ProjectPurchaseJpa persistenceStore;

  @Autowired
  public PurchaseStore(ProjectPurchaseJpa persistenceStore) {
    this.persistenceStore = persistenceStore;
  }

  @Override
  public void storePurchase(ServicePurchase purchase) throws PurchaseStoreException {
    try {
      persistenceStore.save(purchase);
    } catch (RuntimeException e) {
      throw new PurchaseStoreException(
          "Storing the purchase for project %s failed".formatted(purchase.project()), e);
    }
  }

  @Override
  public Iterable<ServicePurchase> storePurchases(List<ServicePurchase> purchases) throws PurchaseStoreException {
    try {
      return persistenceStore.saveAll(purchases);
    } catch (RuntimeException e) {
      throw new PurchaseStoreException("Storing purchases failed.");
    }
  }

  @Override
  public List<Offer> findOffersForProject(ProjectId projectId) {
    try {
      return persistenceStore.findServicePurchasesByProjectIdEquals(projectId).stream()
          .sorted(Comparator.comparing(ServicePurchase::purchasedOn)) //ensures same ordering
          .map(ServicePurchase::getOffer).toList();
    } catch (RuntimeException e) {
      throw new ApplicationException(
          "Retrieving offers for project %s failed.".formatted(projectId), e);
    }
  }

  @Override
  public void deleteOffer(String projectId, long offerId) {
    List<ServicePurchase> purchases = persistenceStore.findServicePurchasesByProjectIdEquals(
        ProjectId.parse(projectId));
    List<ServicePurchase> purchasesWithOffer = purchases.stream()
        .filter(servicePurchase -> servicePurchase.getOffer().id().equals(offerId)).toList();
    persistenceStore.deleteAll(purchasesWithOffer);
  }

  @Override
  public Optional<Offer> findOfferForProject(String projectId, Long offerId) {
    try {
      List<ServicePurchase> purchases = persistenceStore.findServicePurchasesByProjectIdEquals(
          ProjectId.parse(projectId));
      Optional<Offer> foundOffer = purchases.stream()
          .filter(servicePurchase -> servicePurchase.getOffer().id().equals(offerId))
          .map(ServicePurchase::getOffer)
          .findFirst();
      foundOffer.ifPresent(Offer::fileContent); // make sure it is loaded
      return foundOffer;
    } catch (RuntimeException e) {
      throw new ApplicationException(
          "Retrieving offer %d for project %s failed.".formatted(offerId, projectId), e);
    }
  }

  @Override
  public Optional<ProjectId> findProjectIdOfPurchase(Long purchaseID) {
    return persistenceStore.findById(purchaseID).map(ServicePurchase::project);
  }

}
