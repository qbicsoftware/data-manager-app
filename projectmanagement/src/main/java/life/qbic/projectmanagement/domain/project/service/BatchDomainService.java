package life.qbic.projectmanagement.domain.project.service;

import java.util.Collection;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import life.qbic.projectmanagement.domain.project.sample.event.BatchRegistered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class BatchDomainService {

  private final BatchRepository batchRepository;

  @Autowired
  public BatchDomainService(BatchRepository batchRepository) {
    this.batchRepository = batchRepository;
  }

  public Result<Batch, ResponseCode> register(String label, Collection<SampleId> sampleIds,
      boolean isPilot) {
    Batch batch = Batch.create(label, sampleIds, isPilot);

    var result = batchRepository.add(batch);

    result.onValue(theBatch -> DomainEventDispatcher.instance()
        .dispatch(BatchRegistered.create(theBatch.batchId())));

    return result;
  }

  public enum ResponseCode {
    BATCH_REGISTRATION_FAILED
  }


}
