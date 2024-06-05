package life.qbic.projectmanagement.application.policy.directive;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService.ResponseCode;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.event.SampleDeleted;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Delete Sample from Batch</b>
 * <p>
 * After a sample has been deleted, we need to update the batch and remove
 * the sample reference of the deleted sample.
 *
 * @since 1.0.0
 */
@Component
public class DeleteSampleFromBatch implements DomainEventSubscriber<SampleDeleted> {

  private static final Logger log = logger(DeleteSampleFromBatch.class);
  private final BatchRegistrationService batchRegistrationService;
  private final JobScheduler jobScheduler;

  public DeleteSampleFromBatch(BatchRegistrationService batchRegistrationService,
      JobScheduler jobScheduler) {
    this.batchRegistrationService = batchRegistrationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return SampleDeleted.class;
  }

  @Override
  public void handleEvent(SampleDeleted event) {
    jobScheduler.enqueue(() -> deleteSampleFromBatch(event.deletedSample(), event.assignedBatch()));
  }

  @Job(name = "Delete sample %0 from batch %1")
  public void deleteSampleFromBatch(SampleId sample, BatchId batch) throws RuntimeException {
    batchRegistrationService.deleteSampleFromBatch(sample, batch).onError(responseCode -> {
      if (Objects.requireNonNull(responseCode) == ResponseCode.UNKNOWN_BATCH) {
        // This accounts for a batch that might have already been deleted, so nothing is to be done
        log.warn("Cannot delete sample from unknown batch: %s".formatted(batch.value()));
      } else {
        throw new RuntimeException(
            String.format("Deletion of sample %s from batch %s failed, response code was %s ",
                sample,
                batch,
                responseCode));
      }
    });
  }
}
