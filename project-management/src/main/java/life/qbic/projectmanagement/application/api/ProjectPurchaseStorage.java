package life.qbic.projectmanagement.application.api;

import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;

/**
 * <b>Project Purchase Storage</b>
 * <p>
 * Purchase storage interface, that enables storing of {@link ServicePurchase} items.
 * <p>
 * 1.0.0
 */
public interface ProjectPurchaseStorage {

  /**
   * Stores a {@link ServicePurchase} item persistently.
   *
   * @param purchase the service purchase item to store
   * @throws PurchaseStoreException is thrown if the storage fails
   * @since 1.0.0
   */
  void storePurchase(ServicePurchase purchase) throws PurchaseStoreException;

}
