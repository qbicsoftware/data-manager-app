package life.qbic.datamanager.views.projects.project.info;

import reactor.core.publisher.Mono;

/**
 * Service API layer the user interface code shall interact with in the application.
 * <p>
 * The API uses a straight-forward request-response pattern and promotes a reactive service
 * interaction.
 * <p>
 * The interface definition also contains the request and response object records and their body
 * interfaces.
 * <p>
 * Implementing classes must ensure to throw the proper exceptions expected by the client based on
 * the service methods exposed in this interface.
 *
 * @since 1.9.0
 */
public interface AsyncProjectService {

  /**
   * Submits a project update request and returns a reactive {@link Mono<ProjectUpdateResponse>}
   * object immediately.
   * <p>
   * The method is non-blocking.
   * <p>
   * The implementing class must ensure to be able to process all implementing classes of the
   * {@link UpdateRequestBody} interface contained in the request.
   * <p>
   * The implementing class must also ensure to only return responses with classes implementing the
   * {@link UpdateResponseBody} interface.
   *
   * @param request the request to update a project
   * @return a {@link Mono<ProjectUpdateResponse>} object publishing an
   * {@link ProjectUpdateResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException  if the user has insufficient rights
   * @since 1.9.0
   */
  Mono<ProjectUpdateResponse> update(
      ProjectUpdateRequest request) throws UnknownRequestException, RequestFailedException, AccessDeniedException;

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

  class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
      super(message);
    }
    public AccessDeniedException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
