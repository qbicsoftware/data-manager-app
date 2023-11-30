package life.qbic.projectmanagement.infrastructure.project.purchase;

import static org.apache.logging.log4j.LogManager.getLogger;

import life.qbic.projectmanagement.application.api.ProjectPurchaseStorage;
import life.qbic.projectmanagement.application.api.PurchaseStoreException;
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
    } catch (Exception e) {
      log.error("Saving service purchase for project %s failed".formatted(purchase.project()), e);
      throw new PurchaseStoreException("Storing the purchase failed");
    }
  }
}
