package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.InformUsersAboutBatchRegistration;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponBatchCreation;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.sample.event.BatchRegistered;

/**
 * <b>Policy: Batch Registered</b>
 * <p>
 * A collection of all directives that need to be executed after a new batch of
 * samples has been registered for measurement.
 * <p>
 * The policy subscribes to events of type
 * {@link BatchRegistered} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class BatchRegisteredPolicy {

  /**
   * Creates an instance of a {@link BatchRegisteredPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param informUsers   directive to inform users of a project about the new samples of a batch
   *                      {@link Batch}
   * @param updateProject directive to update the project modified timestamp
   * @since 1.0.0
   */
  public BatchRegisteredPolicy(InformUsersAboutBatchRegistration informUsers,
      UpdateProjectUponBatchCreation updateProject) {
    DomainEventDispatcher.instance().subscribe(
        requireNonNull(informUsers, "informUsers must not be null"));
    DomainEventDispatcher.instance().subscribe(
        requireNonNull(updateProject, "updateProject must not be null"));
  }
}
