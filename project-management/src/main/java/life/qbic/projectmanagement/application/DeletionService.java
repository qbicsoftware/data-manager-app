package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.service.BatchDomainService;
import life.qbic.projectmanagement.domain.service.SampleDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Deletion Service</b>
 *
 * <p>Service that orchestrates deletion use cases in the application</p>
 *
 * @since 1.0.0
 */
@Service
public class DeletionService {

  private static final Logger log = logger(DeletionService.class);
  private final ProjectInformationService projectInformationService;
  private final ExperimentInformationService experimentInformationService;
  private final SampleInformationService sampleInformationService;
  private final BatchDomainService batchDomainService;
  private final SampleDomainService sampleDomainService;

  @Autowired
  public DeletionService(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      SampleInformationService sampleInformationService, BatchDomainService batchDomainService,
      SampleDomainService sampleDomainService) {
    this.projectInformationService = requireNonNull(projectInformationService,
        "experimentInformationService must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "experimentInformationService must not be null");
    this.sampleInformationService = requireNonNull(sampleInformationService,
        "sampleInformationService must not be null");
    this.batchDomainService = requireNonNull(batchDomainService,
        BatchDomainService.class.getSimpleName() + " must not be null");
    this.sampleDomainService = requireNonNull(sampleDomainService,
        SampleDomainService.class.getSimpleName() + " must not be null");
  }

  /**
   * Deletes all experiment variables and groups in a given experiment.
   * <p>
   * Will contain an error, if samples are available and attached to the experiment. In this case,
   * no variables and groups will be deleted.
   *
   * @param id the experiment id
   * @return a result containing the experiment id on success or an error {@link ResponseCode}
   * @since 1.0.0
   */
  public Result<ExperimentId, ResponseCode> deleteAllExperimentalVariables(ExperimentId id) {
    var queryResult = sampleInformationService.retrieveSamplesForExperiment(id);
    if (queryResult.isError()) {
      return Result.fromError(ResponseCode.QUERY_FAILED);
    }
    if (queryResult.isValue() && !queryResult.getValue().isEmpty()) {
      return Result.fromError(ResponseCode.SAMPLES_STILL_ATTACHED_TO_EXPERIMENT);
    }
    experimentInformationService.deleteAllExperimentalVariables(id);
    return Result.fromValue(id);
  }

  public Result<ExperimentId, ResponseCode> deleteAllExperimentalGroups(ExperimentId id) {
    var queryResult = sampleInformationService.retrieveSamplesForExperiment(id);
    if (queryResult.isError()) {
      log.debug("experiment (%s) converting %s to %s".formatted(id, queryResult.getError(),
          ResponseCode.QUERY_FAILED));
      return Result.fromError(ResponseCode.QUERY_FAILED);
    }
    if (queryResult.isValue() && !queryResult.getValue().isEmpty()) {
      return Result.fromError(ResponseCode.SAMPLES_STILL_ATTACHED_TO_EXPERIMENT);
    }
    experimentInformationService.deleteAllExperimentalGroups(id);
    return Result.fromValue(id);
  }

  @Transactional
  public Result<BatchId, ResponseCode> deleteBatch(ProjectId projectId, BatchId batchId) {
    var queryResult = sampleInformationService.retrieveSamplesForBatch(batchId);
    if (queryResult.isError()) {
      log.debug("batch (%s) converting %s to %s".formatted(batchId, queryResult.getError(),
          ResponseCode.QUERY_FAILED));
      return Result.fromError(ResponseCode.QUERY_FAILED);
    }
    var result = batchDomainService.deleteBatch(batchId);
    if (result.isError()) {
      return Result.fromError(ResponseCode.BATCH_DELETION_FAILED);
    }
    var sampleDeletionResult = deleteSamples(projectId, queryResult.getValue());
    sampleDeletionResult.onErrorMatching(
        responseCode -> responseCode.equals(ResponseCode.DATA_ATTACHED_TO_SAMPLES),
        ignored -> {
          throw new ApplicationException(ErrorCode.DATA_ATTACHED_TO_SAMPLES,
              ErrorParameters.of(batchId));
        });
    if (sampleDeletionResult.isError()) {
      log.debug("Samples for batch %s could not be deleted due to %s".formatted(batchId,
          sampleDeletionResult.getError()));
      return Result.fromError(ResponseCode.BATCH_DELETION_FAILED);
    }
    return Result.fromValue(batchId);
  }

  @Transactional
  public Result<Collection<Sample>, ResponseCode> deleteSamples(
      ProjectId projectId, Collection<Sample> samplesCollection) {
    var project = projectInformationService.find(projectId);
    if (project.isEmpty()) {
      log.error(
          "Batch deletion aborted. Reason: project with id:" + projectId + " was not found");
      return Result.fromError(ResponseCode.BATCH_DELETION_FAILED);
    }
    var result = sampleDomainService.deleteSamples(project.get(),
        samplesCollection.stream().toList());
    if (result.isError() && result.getError()
        .equals(SampleDomainService.ResponseCode.DATA_ATTACHED_TO_SAMPLES)) {
      return Result.fromError(ResponseCode.DATA_ATTACHED_TO_SAMPLES);
    }
    if (result.isError()) {
      return Result.fromError(ResponseCode.SAMPLE_DELETION_FAILED);
    }
    return Result.fromValue(result.getValue());
  }


  public enum ResponseCode {
    SAMPLES_STILL_ATTACHED_TO_EXPERIMENT, QUERY_FAILED, BATCH_DELETION_FAILED,
    SAMPLE_DELETION_FAILED, DATA_ATTACHED_TO_SAMPLES
  }

}
