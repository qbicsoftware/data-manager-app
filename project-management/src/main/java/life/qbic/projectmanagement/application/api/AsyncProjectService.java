package life.qbic.projectmanagement.application.api;

import static java.util.Objects.nonNull;

import java.util.Optional;
import java.util.UUID;
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
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.9.0
   */
  Mono<ProjectUpdateResponse> update(
      ProjectUpdateRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException;

  /**
   * Container of an update request for a service call and part of the
   * {@link ProjectUpdateRequest}.
   *
   * @since 1.9.0
   */
  sealed interface UpdateRequestBody permits ProjectDesign {

  }

  /**
   * Container of an update response from a service call and part of the
   * {@link ProjectUpdateResponse}.
   *
   * @since 1.9.0
   */
  sealed interface UpdateResponseBody permits ProjectDesign {

  }

  /**
   * Cacheable requests provide a unique identifier so cache implementations can unambiguously
   * manage the requests.
   *
   * @since 1.9.0
   */
  sealed interface CacheableRequest permits ProjectUpdateRequest {

    /**
     * Returns an ID that is unique to the request.
     *
     * @return the id
     * @since 1.9.0
     */
    String requestId();

  }

  /**
   * Container for passing information in an {@link UpdateRequestBody} or
   * {@link UpdateResponseBody}.
   *
   * @param title     the title of the project
   * @param objective the objective of the project
   * @since 1.9.0
   */
  record ProjectDesign(String title, String objective) implements UpdateRequestBody,
      UpdateResponseBody {

  }

  /**
   * A service request to update project information.
   *
   * @param projectId   the project's id
   * @param requestBody the information to be updated.
   * @since 1.9.0
   */
  record ProjectUpdateRequest(String projectId, UpdateRequestBody requestBody, String id) implements
      CacheableRequest {

    public ProjectUpdateRequest(String projectId, UpdateRequestBody requestBody) {
      this(projectId, requestBody, UUID.randomUUID().toString());
    }

    @Override
    public String requestId() {
      return id;
    }
  }

  /**
   * A service response from an update project information request.
   *
   * @param projectId    the project's id
   * @param responseBody the information that was updated.
   * @since 1.9.0
   */
  record ProjectUpdateResponse(String projectId, UpdateResponseBody responseBody, String requestId) {

    public ProjectUpdateResponse {
      if (projectId == null) {
        throw new IllegalArgumentException("Project ID cannot be null");
      }
      if (projectId.isBlank()) {
        throw new IllegalArgumentException("Project ID cannot be blank");
      }
      if (requestId != null && requestId.isBlank()) {
        requestId = null;
      }
    }

    /**
     * Retrieves the request id associated with this response. May be {@link Optional#empty()} but
     * never null.
     *
     * @return an Optional with the requestId; {@link Optional#empty()} otherwise.
     */
    public Optional<String> retrieveRequestId() {
      return Optional.ofNullable(requestId);
    }

    /**
     * Returns the requestId, if no requestId is set, returns an empty String.
     *
     * @return Returns the requestId, if no requestId is set, returns an empty String.
     */
    @Override
    public String requestId() {
      return hasRequestId() ? requestId : "";
    }

    boolean hasRequestId() {
      return nonNull(requestId);
    }
  }

  /**
   * Exception to indicate that the service did not recognise the request.
   *
   * @since 1.9.0
   */
  class UnknownRequestException extends RuntimeException {

    public UnknownRequestException(String message) {
      super(message);
    }
  }

  /**
   * Exception to indicate that the service tried to execute the request, but it failed.
   *
   * @since 1.9.0
   */
  class RequestFailedException extends RuntimeException {

    public RequestFailedException(String message) {
      super(message);
    }

    public RequestFailedException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Exception to indicate that the service tried to execute the request, but the user had
   * insufficient rights and thus the request failed.
   *
   * @since 1.9.0
   */
  class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(String message) {
      super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
