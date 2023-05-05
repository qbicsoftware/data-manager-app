package life.qbic.projectmanagement.application.policy.directive;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class AddSampleToBatch implements DomainEventSubscriber<SampleRegistered> {

  private final BatchRegistrationService batchRegistrationService;

  private final JobScheduler jobScheduler;

  public AddSampleToBatch(BatchRegistrationService batchRegistrationService, JobScheduler jobScheduler) {
    this.batchRegistrationService = batchRegistrationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return SampleRegistered.class;
  }

  @Override
  public void handleEvent(SampleRegistered event) {
    jobScheduler.enqueue(() -> batchRegistrationService.addSampleToBatch(event.registeredSample(), event.assignedBatch()));
  }
}
