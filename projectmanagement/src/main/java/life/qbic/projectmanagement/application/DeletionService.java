package life.qbic.projectmanagement.application;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Deletion Service</b>
 *
 * <p>Service that orchestrates more deletion use cases in the application</p>
 *
 * @since 1.0.0
 */
@Service
public class DeletionService {

  private final ExperimentInformationService experimentInformationService;

  private final SampleInformationService sampleInformationService;

  @Autowired
  public DeletionService(ExperimentInformationService experimentInformationService,
      SampleInformationService sampleInformationService) {
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

  /**
   * Deletes all experiment variables and groups in a given experiment.
   * <p>
   * Will contain an error, if samples are available and attached to the experiment. In this case,
   * to variables and groups will be deleted.
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
    if (queryResult.isValue() && queryResult.getValue().size() > 0) {
      return Result.fromError(ResponseCode.SAMPLES_STILL_ATTACHED_TO_EXPERIMENT);
    }
    experimentInformationService.deleteAllExperimentalVariables(id);
    return Result.fromValue(id);
  }

  public enum ResponseCode {
    SAMPLES_STILL_ATTACHED_TO_EXPERIMENT, QUERY_FAILED
  }


}
