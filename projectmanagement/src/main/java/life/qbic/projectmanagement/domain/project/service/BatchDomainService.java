package life.qbic.projectmanagement.domain.project.service;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.Batch;
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
   * @return a result object with the response. If the registration failed, a response code will be
   * provided.
   * @since 1.0.0
   */
  public Result<Batch, ResponseCode> register(String label, boolean isPilot) {
    Batch batch = Batch.create(label, isPilot);

    var result = batchRepository.add(batch);

    result.onValue(theBatch -> DomainEventDispatcher.instance()
        .dispatch(BatchRegistered.create(theBatch.batchId())));

    return result;
  }

  /**
   * Response error codes for batch registration.
   *
   * @since 1.0.0
   */
  public enum ResponseCode {
    BATCH_REGISTRATION_FAILED
  }


}
