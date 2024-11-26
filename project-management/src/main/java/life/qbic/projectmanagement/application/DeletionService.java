package life.qbic.projectmanagement.application;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.service.BatchDomainService;
import life.qbic.projectmanagement.domain.service.SampleDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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

  private final ApplicationContext context;

  private final ProjectInformationService projectInformationService;
  private final ExperimentInformationService experimentInformationService;
  private final SampleInformationService sampleInformationService;
  private final BatchDomainService batchDomainService;
  private final SampleDomainService sampleDomainService;

  @Autowired
  public DeletionService(ProjectInformationService projectInformationService,
      ExperimentInformationService experimentInformationService,
      SampleInformationService sampleInformationService, BatchDomainService batchDomainService,
      SampleDomainService sampleDomainService, ApplicationContext context) {
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
    this.context = requireNonNull(context);
  }

  /**
   * Deletes all experiment variables and groups in a given experiment.
   * <p>
   * Will contain an error, if samples are available and attached to the experiment. In this case,
   * no variables and groups will be deleted.
   *
   * @param experimentId the experiment id
   * @param projectId the project id
   * @return a result containing the experiment id on success or an error {@link ResponseCode}
   * @since 1.0.0
   */
  public Result<ExperimentId, ResponseCode> deleteAllExperimentalVariables(ExperimentId experimentId,
      ProjectId projectId) {
    var queryResult = sampleInformationService.retrieveSamplesForExperiment(experimentId);
    if (queryResult.isError()) {
      return Result.fromError(ResponseCode.QUERY_FAILED);
    }
    if (queryResult.isValue() && !queryResult.getValue().isEmpty()) {
      return Result.fromError(ResponseCode.SAMPLES_STILL_ATTACHED_TO_EXPERIMENT);
    }
    experimentInformationService.deleteAllExperimentalVariables(experimentId, projectId);
    return Result.fromValue(experimentId);
  }

  @Transactional
  public BatchId deleteBatch(ProjectId projectId, BatchId batchId) {
    var samples = sampleInformationService.retrieveSamplesForBatch(batchId).stream()
        .map(Sample::sampleId).toList();
    var deletedBatchId = batchDomainService.deleteBatch(batchId, projectId);
    deletedBatchId.onError(error -> {
      throw new ApplicationException("Could not delete batch " + batchId);
    });
    // We need to get the proxy Spring has wrapped around the service, otherwise calling
    // the @transaction annotated method has no effect
    context.getBean(DeletionService.class).deleteSamples(projectId, batchId, samples);
    return deletedBatchId.getValue();
  }


  @Transactional
  public void deleteSamples(
      ProjectId projectId, BatchId batchId, Collection<SampleId> samplesCollection) {
    var project = projectInformationService.find(projectId);
    if (project.isEmpty()) {
      throw new IllegalArgumentException("Could not find project " + projectId);
    }
    sampleDomainService.deleteSamples(project.get(), batchId, samplesCollection);
  }

  /**
   * Confirms that a sample can be removed. Might indicate that sample removal is not possible due
   * to the following reasons:
   * <ul>
   *   <li> data is connected to the sample
   *
   * @param sampleId
   * @return
   */
  public boolean isSampleRemovable(SampleId sampleId) {
    return sampleDomainService.isSampleRemovable(sampleId);
  }


  public enum ResponseCode {
    SAMPLES_STILL_ATTACHED_TO_EXPERIMENT, QUERY_FAILED, BATCH_DELETION_FAILED,
    SAMPLE_DELETION_FAILED, DATA_ATTACHED_TO_SAMPLES
  }

}
