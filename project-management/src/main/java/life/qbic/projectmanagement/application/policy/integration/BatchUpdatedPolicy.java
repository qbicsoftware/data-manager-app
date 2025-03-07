package life.qbic.projectmanagement.application.policy.integration;

import static java.util.Objects.requireNonNull;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponBatchUpdate;
import life.qbic.projectmanagement.domain.model.sample.event.BatchUpdated;

/**
 * <b>Policy: Batch Updated</b>
 * <p>
 * A collection of all directives that need to be executed after a batch of
 * samples has been updated.
 * <p>
 * The policy subscribes to events of type
 * {@link BatchUpdated} and ensures the registration of all business required directives.
 *
 * @since 1.0.0
 */
public class BatchUpdatedPolicy {

  /**
   * Creates an instance of a {@link BatchUpdatedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProject directive to update the project modified timestamp
   * @since 1.0.0
   */
  public BatchUpdatedPolicy(UpdateProjectUponBatchUpdate updateProject) {
    DomainEventDispatcher.instance().subscribe(
        requireNonNull(updateProject, "updateProject must not be null"));
  }
}
