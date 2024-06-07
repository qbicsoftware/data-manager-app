package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponMeasurementCreation;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementCreatedEvent;

/**
 * <b>Policy: Measurement Created</b>
 * <p>
 * A collection of all directives that need to be executed after a measurement has been created
 * <p>
 * The policy subscribes to events of type {@link MeasurementCreatedEvent} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class MeasurementCreatedPolicy {

  /**
   * Creates an instance of a {@link MeasurementCreatedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProjectUponMeasurementCreation directive to update the respective project
   *
   * @since 1.0.0
   */
  public MeasurementCreatedPolicy(
      UpdateProjectUponMeasurementCreation updateProjectUponMeasurementCreation) {
    DomainEventDispatcher.instance().subscribe(updateProjectUponMeasurementCreation);
  }
}
