package life.qbic.projectmanagement.application.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import reactor.core.publisher.Mono;

public interface AsyncExperimentService {

  sealed interface UpdateRequestBody permits ConfoundingVariables, ExperimentDescription,
      ExperimentalVariables {

  }

  sealed interface UpdateResponseBody permits ConfoundingVariables, ExperimentDescription,
      ExperimentalVariables {

  }

  record ExperimentalVariable(String name, Set<String> levels, String unit) {

  }

  record ExperimentalVariables(List<ExperimentalVariable> experimentalVariables) implements
      UpdateRequestBody,
      UpdateResponseBody {

  }

  record ExperimentDescription(String experimentName, Set<String> species, Set<String> specimen,
                               Set<String> analytes) implements UpdateRequestBody,
      UpdateResponseBody {


  }

  record ConfoundingVariables(List<ConfoundingVariableInformation> confoundingVariables) implements
      UpdateRequestBody, UpdateResponseBody {

  }

  record ExperimentUpdateRequest(String projectId, String experimentId, UpdateRequestBody body,
                                 String requestId) {

    public ExperimentUpdateRequest(String projectId, String experimentId, UpdateRequestBody body) {
      this(projectId, experimentId, body, UUID.randomUUID().toString());
    }
  }

  record ExperimentUpdateResponse(String experimentId, UpdateResponseBody body, String requestId) {

  }

  Mono<ExperimentUpdateResponse> update(ExperimentUpdateRequest request);

  //
//      experimentalVariableContents.forEach(
//  experimentalVariableContent -> experimentInformationService.addVariableToExperiment(
//      context.projectId().orElseThrow().value(),
//            context.experimentId().orElseThrow(),
//            experimentalVariableContent.name(), experimentalVariableContent.unit(),
//                experimentalVariableContent.levels()));


}
