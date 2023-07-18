package life.qbic.projectmanagement.application;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
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
public class DeletionService {

  private final ExperimentInformationService experimentInformationService;

  private final SampleInformationService sampleInformationService;

  @Autowired
  public DeletionService(ExperimentInformationService experimentInformationService,
      SampleInformationService sampleInformationService) {
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.sampleInformationService = Objects.requireNonNull(sampleInformationService);
  }

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
