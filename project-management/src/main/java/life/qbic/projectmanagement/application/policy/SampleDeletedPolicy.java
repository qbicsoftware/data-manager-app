package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.DeleteSampleFromBatch;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectLastModified;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.sample.event.SampleDeleted;

/**
 * <b>Policy: Sample Deleted</b>
 * <p>
 * A collection of all directives that need to be executed after a sample has been deleted
 * <p>
 * The policy subscribes to events of type
 * {@link SampleDeleted} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class SampleDeletedPolicy {

  /**
   * Creates an instance of a {@link SampleDeletedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param deleteSampleFromBatch directive to remove the affected sample from
   *                         {@link Batch}
   * @param updateProject directive to update the project modified timestamp
   * @since 1.0.0
   */
  public SampleDeletedPolicy(DeleteSampleFromBatch deleteSampleFromBatch,
      UpdateProjectLastModified updateProject) {
    DomainEventDispatcher.instance().subscribe(deleteSampleFromBatch);
    DomainEventDispatcher.instance().subscribe(updateProject);
  }
}
