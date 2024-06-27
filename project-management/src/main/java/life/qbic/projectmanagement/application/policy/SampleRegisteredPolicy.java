package life.qbic.projectmanagement.application.policy;

import static java.util.Objects.requireNonNull;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.AddSampleToBatch;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponSampleCreation;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;

/**
 * <b>Policy: Sample Registered</b>
 * <p>
 * A collection of all directives that need to be executed after a new sample has been registered
 * for measurement.
 * <p>
 * The policy subscribes to events of type
 * {@link SampleRegistered} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class SampleRegisteredPolicy {

  /**
   * Creates an instance of a {@link SampleRegisteredPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param addSampleToBatch directive to update the affected sample {@link Batch}
   * @param updateProject    directive to update the related project
   * @since 1.0.0
   */
  public SampleRegisteredPolicy(AddSampleToBatch addSampleToBatch,
      UpdateProjectUponSampleCreation updateProject) {
    DomainEventDispatcher.instance().subscribe(
        requireNonNull(addSampleToBatch, "addSampleToBatch must not be null"));
    DomainEventDispatcher.instance().subscribe(
        requireNonNull(updateProject, "updateProject must not be null"));
  }
}
