package life.qbic.projectmanagement.application.api;

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
   * Submits a project update request and returns a reactive {@link Mono< ProjectUpdateResponse >}
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


  Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException;


  /**
   * Container of an update request for a service call and part of the
   * {@link ProjectUpdateRequest}.
   *
   * @since 1.9.0
   */
  sealed interface UpdateRequestBody permits FundingInformation, ProjectContacts, ProjectDesign {

  }

  /**
   * Container of an update response from a service call and part of the
   * {@link ProjectUpdateResponse}.
   *
   * @since 1.9.0
   */
  sealed interface UpdateResponseBody permits FundingInformation, ProjectContacts, ProjectDesign {

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
   * Container for passing information about the different project contacts.
   *
   * @param investigator the principal investigator
   * @param manager      the project manager
   * @param responsible  the responsible person
   * @since 1.9.0
   */
  record ProjectContacts(ProjectContact investigator, ProjectContact manager,
                         ProjectContact responsible) implements UpdateRequestBody,
      UpdateResponseBody {

  }

  /**
   * A project contact.
   *
   * @param fullName the full name of the person
   * @param email    a valid email address for contact
   * @since 1.9.0
   */
  record ProjectContact(String fullName, String email) {

  }

  /**
   * Container for funding information of a project.
   *
   * @param grant   the grant name
   * @param grantId the grant ID
   * @since 1.9.0
   */
  record FundingInformation(String grant, String grantId) implements UpdateRequestBody,
      UpdateResponseBody {

  }

  /**
   * A service request to create a project.
   *
   * @param design   the title and objective of a project
   * @param contacts the different contact persons of a project
   * @param funding  some funding information
   * @since 1.9.0
   */
  record ProjectCreationRequest(ProjectDesign design, ProjectContacts contacts,
                                FundingInformation funding) {

  }


  /**
   * A service response from a project creation request
   *
   * @param projectId
   * @since 1.9, 0
   */
  record ProjectCreationResponse(String projectId) {

  }


  /**
   * A service request to update project information.
   *
   * @param projectId   the project's id
   * @param requestBody the information to be updated.
   * @since 1.9.0
   */
  record ProjectUpdateRequest(String projectId, UpdateRequestBody requestBody) {

  }

  /**
   * A service response from an update project information request.
   *
   * @param projectId    the project's id
   * @param responseBody the information that was updated.
   * @since 1.9.0
   */
  record ProjectUpdateResponse(String projectId, UpdateResponseBody responseBody) {

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
