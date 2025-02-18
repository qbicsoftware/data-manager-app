package life.qbic.datamanager.views.projects.project.info;

import reactor.core.publisher.Mono;

public interface AsyncProjectService {

  Mono<ProjectUpdateResponse> update(
      ProjectUpdateRequest projectDesign) throws UnknownRequestException;

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

  class UnknownRequestException extends RuntimeException {
    public UnknownRequestException(String message) {
      super(message);
    }
  }

  class RequestFailedException extends RuntimeException {
    public RequestFailedException(String message) {
      super(message);
    }

    public RequestFailedException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
