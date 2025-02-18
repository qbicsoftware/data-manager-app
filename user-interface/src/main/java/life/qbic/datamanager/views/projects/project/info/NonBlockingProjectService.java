package life.qbic.datamanager.views.projects.project.info;

import reactor.core.publisher.Mono;

public interface NonBlockingProjectService {

  sealed interface UpdateRequestBody permits ProjectDesign {

  }

  sealed interface UpdateResponseBody permits ProjectDesign {


  }

  record ProjectDesign(String title, String objective) implements UpdateRequestBody,
      UpdateResponseBody {

  }

  record ProjectUpdateRequest(String projectId, UpdateRequestBody requestBody) {

  }

  record ProjectUpdateResponse(String projectId, UpdateResponseBody responseBody) {

  }

  Mono<NonBlockingProjectService.ProjectUpdateResponse> update(
      NonBlockingProjectService.ProjectUpdateRequest projectDesign);

}
