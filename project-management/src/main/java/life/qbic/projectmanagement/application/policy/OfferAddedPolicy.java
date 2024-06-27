package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponPurchaseCreation;
import life.qbic.projectmanagement.domain.model.project.purchase.PurchaseCreatedEvent;

/**
 * <b>Policy: Offer Object Added</b>
 * <p>
 * A collection of all directives that need to be executed after an offer object has been created
 * <p>
 * The policy subscribes to events of type {@link PurchaseCreatedEvent} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class OfferAddedPolicy {

  /**
   * Creates an instance of a {@link OfferAddedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProjectUponPurchaseCreation directive to update the respective project
   *
   * @since 1.0.0
   */
  public OfferAddedPolicy(
      UpdateProjectUponPurchaseCreation updateProjectUponPurchaseCreation) {
    DomainEventDispatcher.instance().subscribe(updateProjectUponPurchaseCreation);
  }
}
