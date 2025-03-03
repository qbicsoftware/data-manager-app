package life.qbic.projectmanagement.application.api;

import java.util.List;
import static java.util.Objects.nonNull;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import org.springframework.lang.Nullable;
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
   * The method implementation must be non-blocking.
   * <p>
   * The implementing class must ensure to be able to process all implementing classes of the
   * {@link ProjectUpdateRequestBody} interface contained in the request.
   * <p>
   * The implementing class must also ensure to only return responses with classes implementing the
   * {@link ProjectUpdateResponseBody} interface.
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
   * Submits an experiment update request and returns a reactive
   * {@link Mono< ExperimentUpdateResponse >} object immediately.
   * <p>
   * The method is non-blocking.
   * <p>
   * The implementing class must ensure to be able to process all implementing classes of the
   * {@link ProjectUpdateRequestBody} interface contained in the request.
   * <p>
   * The implementing class must also ensure to only return responses with classes implementing the
   * {@link ProjectUpdateResponseBody} interface.
   *
   * @param request the request to update a project
   * @return a {@link Mono<ProjectUpdateResponse>} object publishing an
   * {@link ProjectUpdateResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.9.0
   */
  Mono<ExperimentUpdateResponse> update(ExperimentUpdateRequest request)
      throws RequestFailedException, AccessDeniedException;



  /**
   * Submits a project creation request and returns a {@link Mono<ProjectCreationResponse>}
   * immediately.
   * <p>
   * This implementation must be non-blocking.
   *
   * @param request the request with information required for project creation.
   * @return {@link Mono<ProjectCreationResponse>} object publishing an
   * {@link ProjectCreationResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.9.0
   */
  Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException;


  /**
   * Container of an update request for a service call and part of the
   * {@link ProjectUpdateRequest}.
   *
   * @since 1.9.0
   */
  sealed interface ProjectUpdateRequestBody permits FundingInformation, ProjectContacts,
      ProjectDesign {

  }

  /**
   * Container of an update response from a service call and part of the
   * {@link ProjectUpdateResponse}.
   *
   * @since 1.9.0
   */
  sealed interface ProjectUpdateResponseBody permits FundingInformation, ProjectContacts,
      ProjectDesign {

  }

  sealed interface ExperimentUpdateRequestBody permits ConfoundingVariables, ExperimentDescription,
      ExperimentalGroups, ExperimentalVariables {

  }

  sealed interface ExperimentUpdateResponseBody permits ConfoundingVariables, ExperimentDescription,
      ExperimentalGroups, ExperimentalVariables {

  }


  /**
   * Cacheable requests provide a unique identifier so cache implementations can unambiguously
   * manage the requests.
   *
   * @since 1.9.0
   */
  sealed interface CacheableRequest permits ProjectUpdateRequest, ExperimentUpdateRequest {

    /**
     * Returns an ID that is unique to the request.
     *
     * @return the id
     * @since 1.9.0
     */
    String requestId();

  }

  /**
   * Container for passing information in an {@link ProjectUpdateRequestBody} or
   * {@link ProjectUpdateResponseBody}.
   *
   * @param title     the title of the project
   * @param objective the objective of the project
   * @since 1.9.0
   */
  record ProjectDesign(String title, String objective) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

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
                         ProjectContact responsible) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

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
  record FundingInformation(String grant, String grantId) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

  }

  /**
   * Contains information on one experimental variables
   *
   * @param name   the name of the variable
   * @param levels possible levels of the variable
   * @param unit   the unit of the experimental variable. Can be null if no unit is set
   * @since 1.9.0
   */
  record ExperimentalVariable(String name, Set<String> levels, @Nullable String unit) {

  }

  /**
   * Container of experimental variables. Can be used in {@link #update(ExperimentUpdateRequest)}.
   * @param experimentalVariables the list of experimental variables
   * @since 1.9.0
   */
  record ExperimentalVariables(
      List<ExperimentalVariable> experimentalVariables) implements
      ExperimentUpdateRequestBody,
      ExperimentUpdateResponseBody {

  }

  /**
   * A level of an experimental variable
   *
   * @param variableName the name of the variable
   * @param levelValue   the value of the level
   * @param unit         the unit for the value of the level. Can be null if no unit is set
   * @since 1.9.0
   */
  record VariableLevel(String variableName, String levelValue, @Nullable String unit) {

  }

  /**
   * Information about an experimental group
   *
   * @param groupId    the identifier of the group
   * @param name       the name of the eperimental group can be empty but is not expected to be
   *                   null
   * @param sampleSize the number of samples in this experimental group
   * @param levels     the experimental variable levels making up the condition for the samples in
   *                   this group.
   * @since 1.9.0
   */
  record ExperimentalGroup(@Nullable Long groupId, String name, int sampleSize,
                           Set<VariableLevel> levels) {

  }

  /**
   * A container for experimental groups. Can be used in {@link #update(ExperimentUpdateRequest)}
   * @param experimentalGroups the list of experimental groups
   * @since 1.9.0
   */
  record ExperimentalGroups(List<ExperimentalGroup> experimentalGroups) implements
      ExperimentUpdateRequestBody,
      ExperimentUpdateResponseBody {

  }

  /**
   * A container describing the experiment
   *
   * @param experimentName the name of the experiment
   * @param species        a set of species for the experiment. Expected textual representations
   *                       containing CURIEs.
   * @param specimen       a set of specimen for the eperiment. Expected textual representations
   *                       containing CURIEs.
   * @param analytes       a set of analytes for the eperiment.Expected textual representations
   *                       containing CURIEs.
   * @since 1.9.0
   */
  record ExperimentDescription(String experimentName, Set<String> species, Set<String> specimen,
                               Set<String> analytes) implements ExperimentUpdateRequestBody,
      ExperimentUpdateResponseBody {


  }

  /**
   * A list of confounding variable information. Can be used in {@link #update(ExperimentUpdateRequest)}
   * @param confoundingVariables the variable information
   */
  record ConfoundingVariables(List<ConfoundingVariableInformation> confoundingVariables) implements
      ExperimentUpdateRequestBody, ExperimentUpdateResponseBody {

  }


  /**
   * A service request to update an experiment
   * @param projectId the project's identifier. The project containing the experiment.
   * @param experimentId the experiment's identifier
   * @param body the request body containing information on what was updated
   * @param requestId The identifier of the request. Please use {@link #ExperimentUpdateRequest(String, String, ExperimentUpdateRequestBody)} if it is not determined yet.
   * @since 1.9.0
   */
  record ExperimentUpdateRequest(String projectId, String experimentId,
                                 ExperimentUpdateRequestBody body,
                                 String requestId) implements CacheableRequest {

    /**
     * A service request to update an experiment
     * @param projectId the project's identifier. The project containing the experiment.
     * @param experimentId the experiment's identifier
     * @param body the request body containing information on what was updated
     * @since 1.9.0
     */
    public ExperimentUpdateRequest(String projectId, String experimentId,
        ExperimentUpdateRequestBody body) {
      this(projectId, experimentId, body, UUID.randomUUID().toString());
    }
  }

  /**
   * A service response from a {@link ExperimentUpdateRequest}
   * @param experimentId the experiment's identifier
   * @param body information about the update
   * @param requestId the identifier of the original request to which this is a response.
   * @since 1.9.0
   */
  record ExperimentUpdateResponse(String experimentId, ExperimentUpdateResponseBody body,
                                  String requestId) {

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
  record ProjectUpdateRequest(String projectId, ProjectUpdateRequestBody requestBody,
                              String id) implements
      CacheableRequest {

    public ProjectUpdateRequest(String projectId, ProjectUpdateRequestBody requestBody) {
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
  record ProjectUpdateResponse(String projectId, ProjectUpdateResponseBody responseBody,
                               String requestId) {

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
     * Returns the requestId, can be null.
     *
     * @return Returns the requestId, if no requestId is set, returns null.
     */
    @Override
    public String requestId() {
      return requestId;
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
