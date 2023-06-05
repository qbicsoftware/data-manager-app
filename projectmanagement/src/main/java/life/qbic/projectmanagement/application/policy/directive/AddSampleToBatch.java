package life.qbic.projectmanagement.application.policy.directive;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b>Directive: Add Sample to Batch</b>
 * <p>
 * After a sample has been registered and assigned to a batch, we need to update the batch and add
 * the sample reference of the newly registered sample.
 *
 * @since 1.0.0
 */
public class AddSampleToBatch implements DomainEventSubscriber<SampleRegistered> {

  private final BatchRegistrationService batchRegistrationService;

  private final JobScheduler jobScheduler;

  public AddSampleToBatch(BatchRegistrationService batchRegistrationService,
      JobScheduler jobScheduler) {
    this.batchRegistrationService = batchRegistrationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return SampleRegistered.class;
  }

  @Override
  public void handleEvent(SampleRegistered event) {
    jobScheduler.enqueue(() -> batchRegistrationService.addSampleToBatch(event.registeredSample(),
        event.assignedBatch()));
  }
}
