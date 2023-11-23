package life.qbic.projectmanagement.application.batch;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.BatchRepository;
import life.qbic.projectmanagement.domain.service.BatchDomainService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Batch Registration Service</b>
 * <p>
 * Service that handles {@link Batch} creation and deletion events, that need to dispatch domain
 * events.
 *
 * @since 1.0.0
 */
@Service
public class BatchRegistrationService {

  private final BatchRepository batchRepository;
  private final BatchDomainService batchDomainService;
  private final ProjectInformationService projectInformationService;
  private static final Logger log = getLogger(BatchRegistrationService.class);

  @Autowired
  public BatchRegistrationService(BatchRepository batchRepository,
      BatchDomainService batchDomainService, ProjectInformationService projectInformationService) {
    this.batchRepository = Objects.requireNonNull(batchRepository);
    this.batchDomainService = Objects.requireNonNull(batchDomainService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
  }

  /**
   * Registers a new batch of samples that serves as reference for sample processing in the lab for
   * measurement and analysis purposes.
   *
   * @param label   a human-readable semantic descriptor of the batch
   * @param isPilot a flag that indicates the batch to describe as pilot submission batch. Pilots
   *                are usually followed by a complete batch that represents the measurements of the
   *                complete experiment.
   * @param projectId id of the project this batch is added to
   * @return a result object with the response. If the registration failed, a response code will be
   * provided.
   * @since 1.0.0
   */
  public Result<BatchId, ResponseCode> registerBatch(String label, boolean isPilot,
      ProjectId projectId) {
    var project = projectInformationService.find(projectId);
    if (project.isEmpty()) {
      log.error("Batch registration aborted. Reason: project with id:"+projectId+" was not found");
      return Result.fromError(ResponseCode.BATCH_CREATION_FAILED);
    }
    String projectTitle = project.get().getProjectIntent().projectTitle().title();
    return batchDomainService.register(label, isPilot, projectTitle, projectId);
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
    BATCH_REGISTRATION_FAILED,
    BATCH_DELETION_FAILED
  }

}
