package life.qbic.projectmanagement.application.api;

import life.qbic.projectmanagement.domain.model.project.purchase.Offer;
import life.qbic.projectmanagement.domain.model.project.purchase.ServicePurchase;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface ProjectPurchaseStorage {

  void storePurchase(ServicePurchase purchase) throws PurchaseStoreException;

}
