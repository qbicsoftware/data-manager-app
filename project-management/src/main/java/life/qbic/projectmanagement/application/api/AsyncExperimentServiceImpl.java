package life.qbic.projectmanagement.application.api;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class AsyncExperimentServiceImpl implements AsyncExperimentService {

  @Override
  public Mono<ExperimentUpdateResponse> update(ExperimentUpdateRequest request) {
    return Mono.fromSupplier(() -> switch (request.body()) {
      case ExperimentalVariables experimentalVariables ->
          updateExperimentalVariables(request.projectId(), request.experimentId(),
              experimentalVariables);
      case ExperimentDescription experimentDescription ->
          updateExperimentDescription(request.projectId(), request.experimentId(),
              experimentDescription);

    }).subscribeOn(Schedulers.boundedElastic());
  }

  private ExperimentUpdateResponse updateExperimentDescription(String projectId,
      String experimentId,
      ExperimentDescription experimentDescription) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  private ExperimentUpdateResponse updateExperimentalVariables(String projectId,
      String experimentId, ExperimentalVariables experimentalVariables) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }
}
