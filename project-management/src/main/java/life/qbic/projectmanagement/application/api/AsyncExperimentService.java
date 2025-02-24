package life.qbic.projectmanagement.application.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface AsyncExperimentService {
//
//      experimentalVariableContents.forEach(
//  experimentalVariableContent -> experimentInformationService.addVariableToExperiment(
//      context.projectId().orElseThrow().value(),
//            context.experimentId().orElseThrow(),
//            experimentalVariableContent.name(), experimentalVariableContent.unit(),
//                experimentalVariableContent.levels()));

  sealed interface UpdateRequestBody permits ExperimentalVariables {

  }

  sealed interface UpdateResponseBody permits ExperimentalVariables {

  }

  record ExperimentalVariable(String name, Set<String> levels, String unit) {

  }

  record ExperimentalVariables(List<ExperimentalVariable> experimentalVariables) implements
      UpdateRequestBody,
      UpdateResponseBody {

  }

  record ExperimentUpdateRequest(String projectId, String experimentId, UpdateRequestBody body,
                                 String requestId) {

    public ExperimentUpdateRequest(String projectId, String experimentId, UpdateRequestBody body) {
      this(projectId, experimentId, body, UUID.randomUUID().toString());
    }
  }

  record ExperimentUpdateResponse(String experimentId, UpdateResponseBody body, String requestId) {


  }

}
