package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponDeletionEvent;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged;

/**
 * <b>Policy: Sample Updated</b>
 * <p>
 * A collection of all directives that need to be executed after a sample has been updated
 * <p>
 * The policy subscribes to events of type
 * {@link ProjectChanged} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class ProjectChangedPolicy {

  /**
   * Creates an instance of a {@link ProjectChangedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProject directive to update the project modified timestamp
   * @since 1.0.0
   */
  public ProjectChangedPolicy(UpdateProjectUponDeletionEvent updateProject) {
    DomainEventDispatcher.instance().subscribe(updateProject);
  }
}
