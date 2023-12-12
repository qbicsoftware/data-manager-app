package life.qbic.projectmanagement.application.api;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.purchase.Offer;
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

  void storePurchases(List<ServicePurchase> purchases) throws PurchaseStoreException;

  List<Offer> findOffersForProject(ProjectId projectId);

  void deleteOffer(String projectId, long offerId);

  Optional<Offer> findOfferForProject(String projectId, Long offerId);

}
