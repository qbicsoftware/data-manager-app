package life.qbic.projectmanagement.infrastructure.project.purchase;

import static org.apache.logging.log4j.LogManager.getLogger;

import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.application.api.ProjectPurchaseStorage;
import life.qbic.projectmanagement.application.api.PurchaseStoreException;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.purchase.Offer;
import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;
import org.apache.logging.log4j.Logger;
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

  private static final Logger log = getLogger(PurchaseStore.class);

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
  public void storePurchases(List<ServicePurchase> purchases) throws PurchaseStoreException {
    try {
      persistenceStore.saveAll(purchases);
    } catch (RuntimeException e) {
      throw new PurchaseStoreException("Storing purchases failed.");
    }
  }

  @Override
  public List<Offer> findOffersForProject(ProjectId projectId) {
    try {
      return persistenceStore.findServicePurchasesByProjectIdEquals(projectId).stream()
          .map(ServicePurchase::getOffer).toList();
    } catch (RuntimeException e) {
      throw new ApplicationException(
          "Retrieving offers for project %s failed.".formatted(projectId), e);
    }
  }
}
