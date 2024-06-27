package life.qbic.projectmanagement.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.policy.directive.UpdateProjectUponMeasurementUpdate;
import life.qbic.projectmanagement.domain.model.measurement.event.MeasurementUpdatedEvent;

/**
 * <b>Policy: Measurement Updated</b>
 * <p>
 * A collection of all directives that need to be executed after a measurement has been updated
 * <p>
 * The policy subscribes to events of type {@link MeasurementUpdatedEvent} and ensures the
 * registration of all business required directives.
 *
 * @since 1.0.0
 */
public class MeasurementUpdatedPolicy {

  /**
   * Creates an instance of a {@link MeasurementUpdatedPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param updateProjectUponMeasurementUpdate directive to update the respective project
   *
   * @since 1.0.0
   */
  public MeasurementUpdatedPolicy(
      UpdateProjectUponMeasurementUpdate updateProjectUponMeasurementUpdate) {
    DomainEventDispatcher.instance().subscribe(updateProjectUponMeasurementUpdate);
  }
}
