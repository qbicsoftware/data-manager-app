package life.qbic.projectmanagement.application.policy.directive;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.event.SampleRegistered;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;
/**
 * <b>Directive: Add Sample to Batch</b>
 * <p>
 * After a sample has been registered and assigned to a batch, we need to update the batch and add
 * the sample reference of the newly registered sample.
 *
 * @since 1.0.0
 */
@Component
public class AddSampleToBatch implements DomainEventSubscriber<SampleRegistered> {

  private static final Logger log = logger(AddSampleToBatch.class);
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
    jobScheduler.enqueue(() -> addSampleToBatch(event.registeredSample(), event.assignedBatch()));
  }

  @Job(name = "Add sample %0 to batch %1")
  public void addSampleToBatch(SampleId sample, BatchId batch) throws RuntimeException {
    batchRegistrationService.addSampleToBatch(sample, batch).onError(responseCode -> {
      throw new RuntimeException(
          String.format("Adding sample %s to batch %s failed, response code was %s ", sample, batch,
              responseCode));
    });
  }
}
