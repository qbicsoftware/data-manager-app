package life.qbic.projectmanagement.application.batch;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.event.ProjectChanged;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.repository.BatchRepository;
import life.qbic.projectmanagement.domain.service.BatchDomainService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

  private static final Logger log = getLogger(BatchRegistrationService.class);
  private final BatchRepository batchRepository;
  private final BatchDomainService batchDomainService;
  private final ProjectInformationService projectInformationService;
  private final SampleInformationService sampleInformationService;
  private final SampleRegistrationService sampleRegistrationService;
  private final DeletionService deletionService;

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
    var random = new Random();
    while (true) {
      try {
        return tryToUpdateBatch(sampleId, batchId);
      } catch (ObjectOptimisticLockingFailureException e) {
        log.debug(
            "Batch with id \"%s\" was already updated in between, try to read the batch again".formatted(
                batchId.value()));
      }
      try {
        Thread.sleep(random.nextInt(500));
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Result<BatchId, ResponseCode> tryToUpdateBatch(SampleId sampleId,
      BatchId batchId) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      log.error("cannot find batch with id " + batchId.value());
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

  public Result<BatchId, ResponseCode> deleteSampleFromBatch(SampleId sampleId, BatchId batchId) {
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
      Collection<SampleRegistrationRequest> createdSamples,
      Collection<SampleUpdateRequest> editedSamples,
      Collection<SampleId> deletedSamples, ProjectId projectId) {
    var searchResult = batchRepository.find(batchId);
    if (searchResult.isEmpty()) {
      return Result.fromError(ResponseCode.BATCHES_COULD_NOT_BE_RETRIEVED);
    }

    var samplesInBatch = sampleInformationService.retrieveSamplesForBatch(batchId).stream()
        .map(Sample::sampleId)
        .toList();
    if (editedSamples.stream().map(SampleUpdateRequest::sampleId)
        .anyMatch(it -> !samplesInBatch.contains(it))) {
      return Result.fromError(ResponseCode.SAMPLES_DONT_BELONG_TO_BATCH);
    }

    if (deletedSamples.stream()
        .anyMatch(it -> !samplesInBatch.contains(it))) {
      return Result.fromError(ResponseCode.SAMPLES_DONT_BELONG_TO_BATCH);
    }
    Batch batch = searchResult.get();
    Result<BatchId, ResponseCode> updateResult =
        updateBatchInformation(batch, projectId, batchLabel, isPilot);
    if (!createdSamples.isEmpty()) {
      sampleRegistrationService.registerSamples(createdSamples, projectId);
    }
    if (!editedSamples.isEmpty()) {
      sampleRegistrationService.updateSamples(projectId, editedSamples);
    }
    if (!deletedSamples.isEmpty()) {
      deletionService.deleteSamples(projectId, batchId, deletedSamples);
    }
    return Result.fromValue(batch.batchId());
  }

  private void dispatchSuccessfulBatchUpdate(ProjectId projectId) {
    ProjectChanged projectChanged = ProjectChanged.create(projectId);
    DomainEventDispatcher.instance().dispatch(projectChanged);
  }

  private Result<BatchId, ResponseCode> updateBatchInformation(Batch batch, ProjectId projectId,
      String updatedBatchLabel, boolean updatedIsPilot) {
    batch.setPilot(updatedIsPilot);
    batch.setLabel(updatedBatchLabel);
    var result = batchRepository.update(batch);
    if (result.isValue()) {
      dispatchSuccessfulBatchUpdate(projectId);
      return Result.fromValue(batch.batchId());
    } else {
      return Result.fromError(ResponseCode.BATCH_UPDATE_FAILED);
    }
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
