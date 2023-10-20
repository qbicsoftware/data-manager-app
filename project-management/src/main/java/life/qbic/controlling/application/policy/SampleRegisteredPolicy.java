package life.qbic.controlling.application.policy;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.controlling.application.policy.directive.AddSampleToBatch;
import life.qbic.controlling.domain.model.batch.Batch;
import life.qbic.controlling.domain.model.sample.event.SampleRegistered;

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

  private final AddSampleToBatch addSampleToBatch;

  /**
   * Creates an instance of a {@link SampleRegisteredPolicy} object.
   * <p>
   * All directives will be created and subscribed upon instantiation.
   *
   * @param addSampleToBatch directive to update the affected sample
   *                         {@link Batch}
   * @since 1.0.0
   */
  public SampleRegisteredPolicy(AddSampleToBatch addSampleToBatch) {
    this.addSampleToBatch = addSampleToBatch;
    DomainEventDispatcher.instance().subscribe(this.addSampleToBatch);
  }
}
