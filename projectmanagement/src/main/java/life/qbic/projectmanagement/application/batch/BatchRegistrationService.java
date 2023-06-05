package life.qbic.projectmanagement.application.batch;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
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
public class BatchRegistrationService {

  private final BatchRepository batchRepository;

  public BatchRegistrationService(@Autowired BatchRepository batchRepository) {
    this.batchRepository = batchRepository;
  }

  public Result<BatchId, ResponseCode> registerBatch(String label, boolean isPilot) {
    Batch batch = Batch.create(label, isPilot);
    var result = batchRepository.add(batch);
    if (result.isError()) {
      return Result.fromError(ResponseCode.BATCH_CREATION_FAILED);
    }
    return Result.fromValue(batch.batchId());
  }

  public Result<BatchId, ResponseCode> addSampleToBatch(SampleId sampleId, BatchId batchId) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      return Result.fromError(ResponseCode.BATCH_NOT_FOUND);
    }
    searchResult.ifPresent(batch -> {
      batch.addSample(sampleId);
      batchRepository.update(batch);
    });
    return Result.fromValue(batchId);
  }

  public enum ResponseCode {
    BATCH_UPDATE_FAILED,
    BATCH_NOT_FOUND,
    BATCH_CREATION_FAILED

  }

}
