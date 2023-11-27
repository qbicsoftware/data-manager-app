package life.qbic.projectmanagement.application.batch;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.repository.BatchRepository;
import life.qbic.projectmanagement.domain.service.BatchDomainService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  private final SampleInformationService sampleInformationService;
  private final SampleRegistrationService sampleRegistrationService;
  private final DeletionService deletionService;
  private static final Logger log = getLogger(BatchRegistrationService.class);

  @Autowired
  public BatchRegistrationService(BatchRepository batchRepository,
      BatchDomainService batchDomainService, ProjectInformationService projectInformationService,
      SampleInformationService sampleInformationService,
      SampleRegistrationService sampleRegistrationService, DeletionService deletionService) {
    this.batchRepository = Objects.requireNonNull(batchRepository);
    this.batchDomainService = Objects.requireNonNull(batchDomainService);
    this.projectInformationService = Objects.requireNonNull(projectInformationService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
    this.sampleRegistrationService = Objects.requireNonNull(sampleRegistrationService);
    this.deletionService = Objects.requireNonNull(deletionService);
  }

  /**
   * Registers a new batch of samples that serves as reference for sample processing in the lab for
   * measurement and analysis purposes.
   *
   * @param label     a human-readable semantic descriptor of the batch
   * @param isPilot   a flag that indicates the batch to describe as pilot submission batch. Pilots
   *                  are usually followed by a complete batch that represents the measurements of
   *                  the complete experiment.
   * @param projectId id of the project this batch is added to
   * @return a result object with the response. If the registration failed, a response code will be
   * provided.
   * @since 1.0.0
   */
  public Result<BatchId, ResponseCode> registerBatch(String label, boolean isPilot,
      ProjectId projectId) {
    //Todo move sample Registration logic in here to ensure transactional validity
    var project = projectInformationService.find(projectId);
    if (project.isEmpty()) {
      log.error(
          "Batch registration aborted. Reason: project with id:" + projectId + " was not found");
      return Result.fromError(ResponseCode.BATCH_CREATION_FAILED);
    }
    String projectTitle = project.get().getProjectIntent().projectTitle().title();
    var result = batchDomainService.register(label, isPilot, projectTitle, projectId);
    if (result.isError()) {
      return Result.fromError(ResponseCode.BATCH_REGISTRATION_FAILED);
    }
    return Result.fromValue(result.getValue());
  }

  public Result<BatchId, ResponseCode> addSampleToBatch(SampleId sampleId, BatchId batchId) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      return Result.fromError(ResponseCode.BATCHES_COULD_NOT_BE_RETRIEVED);
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
  //Todo should this be a directive or done manually (maybe remove batch is enough?)
  public Result<BatchId, ResponseCode> removeSampleFromBatch(SampleId sampleId, BatchId batchId) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      return Result.fromError(ResponseCode.BATCHES_COULD_NOT_BE_RETRIEVED);
    } else {
      Batch batch = searchResult.get();
      batch.removeSample(sampleId);
      var result = batchRepository.update(batch);
      if (result.isError()) {
        return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
      }
      return Result.fromValue(batch.batchId());
    }
  }

  /**
   * Edits the information contained within a {@link Batch} and its corresponding registered
   * {@link Sample}
   *
   * @param batchId        a human-readable semantic descriptor of the batch
   * @param batchLabel     updated label of the batch
   * @param isPilot        updated flag that indicates the batch to describe as pilot submission
   *                       batch. Pilots are usually followed by a complete batch that represents
   *                       the measurements of the complete experiment.
   * @param createdSamples Collection of {@link SampleRegistrationRequest}, which do not exist and
   *                       the corresponding {@link Sample} should be created and associated with
   *                       the provided {@link Batch}
   * @param editedSamples  Collection of {@link Sample}, for which the information has changed and
   *                       should be updated within the provided {@link Batch}
   * @param deletedSamples Collection of {@link Sample} which are to be deleted and their
   *                       association with the provided {@link Batch} is to be deleted
   * @param projectId      id of the project the edited {@link Batch} belongs to
   * @return a result object with the response. If the editing failed, a response code will be
   * provided.
   */
  @Transactional
  public Result<BatchId, ResponseCode> editBatch(BatchId batchId, String batchLabel,
      boolean isPilot,
      Collection<SampleRegistrationRequest> createdSamples, Collection<Sample> editedSamples,
      Collection<Sample> deletedSamples, ProjectId projectId) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      return Result.fromError(ResponseCode.BATCHES_COULD_NOT_BE_RETRIEVED);
    }
    Batch batch = searchResult.get();
    updateBatchInformation(batch, batchLabel, isPilot);
    createSamplesInBatch(projectId, batch, createdSamples);
    updateSamplesInBatch(projectId, batch, editedSamples);
    deleteSamplesInBatch(projectId, batch, deletedSamples);
    return Result.fromValue(batch.batchId());
  }

  private Result<BatchId, ResponseCode> updateBatchInformation(Batch batch,
      String updatedBatchLabel,
      boolean updatedIsPilot) {
    batch.setPilot(updatedIsPilot);
    batch.setLabel(updatedBatchLabel);
    var result = batchRepository.update(batch);
    if (result.isValue()) {
      return Result.fromValue(batch.batchId());
    } else {
      return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
    }
  }

  private Result<BatchId, ResponseCode> createSamplesInBatch(ProjectId projectId, Batch batch,
      Collection<SampleRegistrationRequest> createdSamples) {
    if (createdSamples.isEmpty()) {
      return Result.fromValue(batch.batchId());
    }
    var result = sampleRegistrationService.registerSamples(createdSamples, projectId);
    if (result.isValue()) {
      return Result.fromValue(batch.batchId());
    } else {
      return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
    }
  }

  private Result<BatchId, ResponseCode> updateSamplesInBatch(ProjectId projectId, Batch batch,
      Collection<Sample> editedSamples) {
    if (editedSamples.isEmpty()) {
      return Result.fromValue(batch.batchId());
    }
    if (doSamplesBelongToBatch(batch.batchId(), editedSamples)) {
      var result = sampleRegistrationService.updateSamples(projectId, editedSamples);
      if (result.isValue()) {
        return Result.fromValue(batch.batchId());
      } else {
        return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
      }
    } else {
      return Result.fromError(ResponseCode.SAMPLES_DONT_BELONG_TO_BATCH);
    }
  }

  private Result<BatchId, ResponseCode> deleteSamplesInBatch(ProjectId projectId, Batch batch,
      Collection<Sample> deletedSamples) {
    if (deletedSamples.isEmpty()) {
      return Result.fromValue(batch.batchId());
    }
    if (doSamplesBelongToBatch(batch.batchId(), deletedSamples)) {
      var result = deletionService.deleteSamples(projectId, deletedSamples);
      if (result.isValue()) {
        return Result.fromValue(batch.batchId());
      } else {
        return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
      }
    } else {
      return Result.fromError(ResponseCode.SAMPLES_DONT_BELONG_TO_BATCH);
    }
  }

  private boolean doSamplesBelongToBatch(BatchId batchId, Collection<Sample> samples) {
    /* an alternative would be to get the IDs from the batch object directly however
    this might not be updated to the current state */
    var queryResult = sampleInformationService.retrieveSamplesForBatch(batchId);
    if (queryResult.isValue()) {
      return queryResult.getValue().containsAll(samples);
    }
    return false;
  }


  public enum ResponseCode {
    QUERY_FAILED,
    BATCH_UPDATE_FAILED,
    BATCHES_COULD_NOT_BE_RETRIEVED,
    BATCH_CREATION_FAILED,
    BATCH_REGISTRATION_FAILED,
    BATCH_DELETION_FAILED,
    SAMPLES_DONT_BELONG_TO_BATCH
  }

}
