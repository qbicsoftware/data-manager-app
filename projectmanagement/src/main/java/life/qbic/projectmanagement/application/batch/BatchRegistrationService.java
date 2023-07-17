package life.qbic.projectmanagement.application.batch;

import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import life.qbic.projectmanagement.domain.project.service.BatchDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Batch Registration Service
 * <p>
 * Application service handling the registration, deletion, update and retrieval of batch
 * Information
 */
@Service
public class BatchRegistrationService {

  private final BatchRepository batchRepository;
  private final BatchDomainService batchDomainService;

  public BatchRegistrationService(@Autowired BatchRepository batchRepository,
      @Autowired BatchDomainService batchDomainService) {
    this.batchRepository = batchRepository;
    this.batchDomainService = batchDomainService;
  }

  public Result<BatchId, ResponseCode> registerBatch(String label, boolean isPilot) {
    Batch batch = Batch.create(label, isPilot);
    var result = batchDomainService.register(label, isPilot);
    if (result.isError()) {
      return Result.fromError(ResponseCode.BATCH_CREATION_FAILED);
    }
    return Result.fromValue(batch.batchId());
  }

  public Result<BatchId, ResponseCode> addSampleToBatch(SampleId sampleId, BatchId batchId) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      return Result.fromError(ResponseCode.BATCH_NOT_FOUND);
    } else {
      Batch batch = searchResult.get();
      batch.addSample(sampleId);
      var result = batchRepository.update(batch);
      if (result.isError()) {
        return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
      }
      return Result.fromValue(batch.batchId());
    }
  }

  public Result<BatchId, ResponseCode> deleteBatch(BatchId batchId) {
    var result = batchDomainService.deleteBatch(batchId);
    if (result.isError()) {
      return Result.fromError(ResponseCode.BATCH_DELETION_FAILED);
    }
    return Result.fromValue(batchId);
  }

  public Result<BatchId, ResponseCode> updateBatch(BatchId batchId, String batchLabel) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      return Result.fromError(ResponseCode.BATCH_NOT_FOUND);
    } else {
      Batch batch = searchResult.get();
      if (!batch.label().equals(batchLabel)) {
        batch.setLabel(batchLabel);
      }
      var result = batchRepository.update(batch);
      if (result.isError()) {
        return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
      }
      return Result.fromValue(batch.batchId());
    }
  }

  public enum ResponseCode {
    BATCH_UPDATE_FAILED,
    BATCH_NOT_FOUND,
    BATCH_CREATION_FAILED,
    BATCH_DELETION_FAILED
  }

}
