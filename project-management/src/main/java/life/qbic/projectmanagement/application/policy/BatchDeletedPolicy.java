package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectLastModified;
import life.qbic.projectmanagement.domain.model.sample.event.BatchDeleted;

/**
 * <b>Policy: Batch Deleted</b>
 * <p>
 * A collection of all directives that need to be executed after a sample batch
 * has been deleted.
 * <p>
 * The policy subscribes to events of type
 * {@link BatchDeleted} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class BatchDeletedPolicy {

  /**
   * Creates an instance of a {@link BatchDeletedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProject directive to update the project modified timestamp
   * @since 1.0.0
   */
  public BatchDeletedPolicy(UpdateProjectLastModified updateProject) {
    DomainEventDispatcher.instance().subscribe(
        requireNonNull(updateProject, "updateProject must not be null"));
  }
}
