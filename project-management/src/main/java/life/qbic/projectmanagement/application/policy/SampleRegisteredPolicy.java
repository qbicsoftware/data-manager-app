package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponSampleCreation;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;

/**
 * <b>Policy: Sample Registered</b>
 * <p>
 * A collection of all directives that need to be executed after a sample has been created.
 * <p>
 * The policy subscribes to events of type {@link SampleRegistered} and ensures the registration of
 * all business required directives.
 *
 * @since 1.0.0
 */
public class SampleRegisteredPolicy {

  /**
   * Creates an instance of a {@link SampleRegisteredPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProjectUponSampleCreation directive to update the respective project
   *
   * @since 1.0.0
   */
  public SampleRegisteredPolicy(
      UpdateProjectUponSampleCreation updateProjectUponSampleCreation) {
    DomainEventDispatcher.instance().subscribe(updateProjectUponSampleCreation);
  }
}