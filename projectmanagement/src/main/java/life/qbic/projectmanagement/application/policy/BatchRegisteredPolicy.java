package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch;
import life.qbic.projectmanagement.application.policy.directive.InformUsersAboutBatchRegistration;

/**
 * <b>Policy: Batch Registered</b>
 * <p>
 * A collection of all directives that need to be executed after a new batch of
 * samples has been registered for measurement.
 * <p>
 * The policy subscribes to events of type
 * {@link life.qbic.projectmanagement.domain.project.sample.event.BatchRegistered} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class BatchRegisteredPolicy {

  private final InformUsersAboutBatchRegistration informUsers;

  /**
   * Creates an instance of a {@link BatchRegisteredPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param informUsers directive to inform users of a project about the new samples of a batch
   *                         {@link life.qbic.projectmanagement.domain.project.sample.Batch}
   * @since 1.0.0
   */
  public BatchRegisteredPolicy(InformUsersAboutBatchRegistration informUsers) {
    this.informUsers = informUsers;
    DomainEventDispatcher.instance().subscribe(this.informUsers);
  }
}
