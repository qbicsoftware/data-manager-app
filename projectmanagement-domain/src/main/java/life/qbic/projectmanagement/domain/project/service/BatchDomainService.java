package life.qbic.projectmanagement.domain.project.service;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService.ResponseCode;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.event.BatchRegistered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Batch Domain Service</b>
 * <p>
 * Service that handles {@link Batch} creation and deletion events, that need to dispatch domain
 * events.
 *
 * @since 1.0.0
 */
@Service
public class BatchDomainService {

  private final BatchRepository batchRepository;

  @Autowired
  public BatchDomainService(BatchRepository batchRepository) {
    this.batchRepository = Objects.requireNonNull(batchRepository);
  }

  /**
   * Registers a new batch of samples that serves as reference for sample processing in the lab for
   * measurement and analysis purposes.
   *
   * @param label   a human-readable semantic descriptor of the batch
   * @param isPilot a flag that indicates the batch to describe as pilot submission batch. Pilots
   *                are usually followed by a complete batch that represents the measurements of the
   *                complete experiment.
   * @param project the Project the batch is added to
   * @return a result object with the response. If the registration failed, a response code will be
   * provided.
   * @since 1.0.0
   */
  public Result<BatchId, ResponseCode> register(String label, boolean isPilot, Project project) {
    Batch batch = Batch.create(label, isPilot);
    var result = batchRepository.add(batch);
    if (result.isError()) {
      return Result.fromError(ResponseCode.BATCH_CREATION_FAILED);
    } else {
      dispatchSuccessfulSampleBatchRegistration(label, batch.batchId(), project);
    }
    return Result.fromValue(result.getValue().batchId());
  }


  private void dispatchSuccessfulSampleBatchRegistration(String name, BatchId id, Project project) {
    BatchRegistered batchRegistered = BatchRegistered.create(name, id, project);
    DomainEventDispatcher.instance().dispatch(batchRegistered);
  }
}
