package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
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
  public BatchId deleteBatch(ProjectId projectId, BatchId batchId) {
    var samples = sampleInformationService.retrieveSamplesForBatch(batchId).stream()
        .map(Sample::sampleId).toList();
    var deletedBatchId = batchDomainService.deleteBatch(batchId);
    deletedBatchId.onError(error -> {
      throw new ApplicationException("Could not delete batch " + batchId);
    });
    deleteSamples(projectId, samples);
    return deletedBatchId.getValue();
  }


  @Transactional
  public void deleteSamples(
      ProjectId projectId, Collection<SampleId> samplesCollection) {
    var project = projectInformationService.find(projectId);
    if (project.isEmpty()) {
      throw new IllegalArgumentException("Could not find project " + projectId);
    }
    sampleDomainService.deleteSamples(project.get(), samplesCollection);
  }


  public enum ResponseCode {
    SAMPLES_STILL_ATTACHED_TO_EXPERIMENT, QUERY_FAILED, BATCH_DELETION_FAILED,
    SAMPLE_DELETION_FAILED, DATA_ATTACHED_TO_SAMPLES
  }

}
