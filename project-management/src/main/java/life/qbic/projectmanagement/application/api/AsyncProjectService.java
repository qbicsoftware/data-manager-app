package life.qbic.projectmanagement.application.api;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest.SampleInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
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

  //<editor-fold desc="project-related API">

  /**
   * A service request to create a project.
   *
   * @param offerId     the potentially linked offer Id.
   * @param design      the title and objective of a project
   * @param contacts    the different contact persons of a project
   * @param funding     some funding information
   * @param projectCode the desired project code for the project
   * @param requestId   the unique identifier of the request
   * @since 1.9.0
   */
  record ProjectCreationRequest(String offerId, ProjectDesign design, ProjectContacts contacts,
                                FundingInformation funding, String projectCode, String requestId) {

    public ProjectCreationRequest(ProjectDesign design, ProjectContacts contacts,
        String projectCode, String requestId) {
      this(null, design, contacts, null, projectCode, requestId);
    }

    public ProjectCreationRequest {
      requireNonNull(design);
      requireNonNull(contacts);
      requireNonNull(requestId);
      requireNonNull(projectCode);
    }

    /**
     * {@inheritDoc} Please use {@link ProjectCreationRequest#optionalFundingInformation()} to
     * access the funding information safely.
     *
     * @return the funding information. Might be null.
     * @see ProjectCreationRequest#optionalFundingInformation()
     * @since 1.10.0
     */
    @Override
    public FundingInformation funding() {
      return funding;
    }

    public Optional<FundingInformation> optionalFundingInformation() {
      return Optional.ofNullable(funding);
    }
  }

  /**
   * A service response from a project creation request
   *
   * @param projectId
   * @since 1.9, 0
   */
  record ProjectCreationResponse(String projectId, String requestId) {

    public ProjectCreationResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
    }
  }

  /**
   * A service request to update project information.
   *
   * @param projectId   the project's id
   * @param requestBody the information to be updated.
   * @param requestId   the request ID, needs to be provided by the client and will be referenced in
   *                    the response.
   * @since 1.9.0
   */
  record ProjectUpdateRequest(String projectId, ProjectUpdateRequestBody requestBody,
                              String requestId) implements CacheableRequest {

    public ProjectUpdateRequest(String projectId, ProjectUpdateRequestBody requestBody) {
      this(projectId, requestBody, UUID.randomUUID().toString());
    }

    public ProjectUpdateRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(requestBody);
    }

  }

  /**
   * A service response from an update project information request.
   *
   * @param projectId    the project's id
   * @param responseBody the information that was updated.
   * @param requestId    the request ID, needs to be provided by the client and will be referenced
   *                     in the response.
   * @since 1.9.0
   */
  record ProjectUpdateResponse(String projectId, ProjectUpdateResponseBody responseBody,
                               String requestId) {

    public ProjectUpdateResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(responseBody);
    }

  }

  /**
   * Container of an update request for a service call and part of the
   * {@link ProjectUpdateRequest}.
   *
   * @since 1.9.0
   */
  sealed interface ProjectUpdateRequestBody permits FundingInformation, PrincipalInvestigator,
      ProjectDesign, ProjectManager, ProjectResponsible {

  }

  /**
   * Container of an update response from a service call and part of the
   * {@link ProjectUpdateResponse}.
   *
   * @since 1.9.0
   */
  sealed interface ProjectUpdateResponseBody permits FundingInformation,
      PrincipalInvestigator, ProjectDesign, ProjectManager, ProjectResponsible {

  }

  record ProjectDeletionRequest(String projectId, String requestId) {

    public ProjectDeletionRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
    }

    public ProjectDeletionRequest(String projectId) {
      this(projectId, UUID.randomUUID().toString());
    }
  }

  record ProjectDeletionResponse(String projectId, String requestId) {

    public ProjectDeletionResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
    }
  }

  sealed interface ProjectDeletionRequestBody permits FundingDeletion,
      ProjectResponsibleDeletion {

  }

  record ProjectResponsibleDeletion() implements ProjectDeletionRequestBody {

  }

  record FundingInformationCreationRequest(String projectId,
                                           FundingInformation information,
                                           String requestId) implements
      CacheableRequest {

    public FundingInformationCreationRequest {
      requireNonNull(requestId);
      requireNonNull(projectId);
      requireNonNull(information);
    }

    public FundingInformationCreationRequest(String projectId, FundingInformation information) {
      this(projectId, information, UUID.randomUUID().toString());
    }
  }

  record FundingInformationCreationResponse(String requestId, FundingInformation fundingInformation,
                                            String projectId) {

    public FundingInformationCreationResponse {
      requireNonNull(requestId);
      requireNonNull(projectId);
      requireNonNull(fundingInformation);
    }

  }

  record FundingDeletion() implements ProjectDeletionRequestBody {

  }

  record FundingInformationDeletionRequest(String projectId, String requestId) {

    public FundingInformationDeletionRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
    }

    public FundingInformationDeletionRequest(String projectId) {
      this(projectId, UUID.randomUUID().toString());
    }
  }

  record FundingInformationDeletionResponse(String projectId, String requestId) {

    public FundingInformationDeletionResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
    }
  }

  record ProjectResponsibleCreationRequest(String projectId, ProjectContact projectResponsible,
                                           String requestId) implements CacheableRequest {

    public ProjectResponsibleCreationRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(projectResponsible);
    }

    public ProjectResponsibleCreationRequest(String projectId, ProjectContact projectResponsible) {
      this(projectId, projectResponsible, UUID.randomUUID().toString());
    }
  }

  record ProjectResponsibleCreationResponse(String projectId, ProjectContact projectResponsible,
                                            String requestId) {

    public ProjectResponsibleCreationResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(projectResponsible);
    }
  }

  record ProjectResponsibleDeletionRequest(String projectId, String requestId) implements
      CacheableRequest {

    public ProjectResponsibleDeletionRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
    }

    public ProjectResponsibleDeletionRequest(String projectId) {
      this(projectId, UUID.randomUUID().toString());
    }
  }

  record ProjectResponsibleDeletionResponse(String projectId, String requestId) {

    public ProjectResponsibleDeletionResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
    }
  }

  /**
   * A service request to create an experiment
   *
   * @param projectId             the project in which to create the experiment
   * @param experimentDescription the minimal required information for the experiment
   * @param requestId             the unique id of this request. If none exists use
   *                              {@link ExperimentCreationRequest#ExperimentCreationRequest(String,
   *                              ExperimentDescription)} for construction.
   * @since 1.10.0
   */
  record ExperimentCreationRequest(String projectId, ExperimentDescription experimentDescription,
                                   String requestId) implements CacheableRequest {

    /**
     * A service request to create an experiment
     *
     * @param projectId             the project in which to create the experiment
     * @param experimentDescription the minimal required information for the experiment
     * @param requestId             the unique id of this request. If none exists use
     *                              {@link
     *                              ExperimentCreationRequest#ExperimentCreationRequest(String,
     *                              ExperimentDescription)} for construction.
     * @since 1.10.0
     */
    public ExperimentCreationRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(experimentDescription);
    }

    /**
     * A service request to create an experiment. Generates a request id and assinges it to this
     * request.
     *
     * @param projectId             the project in which to create the experiment
     * @param experimentDescription the minimal required information for the experiment
     * @since 1.10.0
     */
    public ExperimentCreationRequest(String projectId,
        ExperimentDescription experimentDescription) {
      this(projectId, experimentDescription, UUID.randomUUID().toString());
    }
  }

  /**
   * A service response for experiment creation.
   *
   * @param projectId             the project in which the experiment was created
   * @param experimentId          the identifier of the created experiment
   * @param experimentDescription information about the experiment
   * @param requestId             the identifier of the original request
   * @since 1.10.0
   */
  record ExperimentCreationResponse(String projectId, String experimentId,
                                    ExperimentDescription experimentDescription,
                                    String requestId) {

    public ExperimentCreationResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(experimentDescription);
    }
  }

  /**
   * A service request to update an experiment
   *
   * @param projectId    the project's identifier. The project containing the experiment.
   * @param experimentId the experiment's identifier
   * @param body         the request body containing information on what was updated
   * @param requestId    the request ID, needs to be provided by the client and will be referenced
   *                     in the response.
   * @since 1.9.0
   */
  record ExperimentUpdateRequest(String projectId, String experimentId,
                                 ExperimentUpdateRequestBody body, String requestId) implements
      CacheableRequest {

    /**
     * A service request to update an experiment
     *
     * @param projectId    the project's identifier. The project containing the experiment.
     * @param experimentId the experiment's identifier
     * @param body         the request body containing information on what was updated
     * @since 1.9.0
     */
    public ExperimentUpdateRequest(String projectId, String experimentId,
        ExperimentUpdateRequestBody body) {
      this(projectId, experimentId, body, UUID.randomUUID().toString());
    }

    public ExperimentUpdateRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(requestId);
      requireNonNull(body);
    }
  }


  /**
   * A service response from a {@link ExperimentUpdateRequest}
   *
   * @param experimentId the experiment's identifier
   * @param body         information about the update
   * @param requestId    the identifier of the original request to which this is a response.
   * @since 1.9.0
   */
  record ExperimentUpdateResponse(String experimentId, ExperimentUpdateResponseBody body,
                                  String requestId) {

    public ExperimentUpdateResponse {
      requireNonNull(experimentId);
      requireNonNull(requestId);
      requireNonNull(body);
    }
  }

  record ExperimentDeletionRequest(String projectId, String experimentId,
                                   String requestId) implements CacheableRequest {

    public ExperimentDeletionRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }

    public ExperimentDeletionRequest(String projectId, String experimentId) {
      this(projectId, experimentId, UUID.randomUUID().toString());
    }
  }

  record ExperimentDeletionResponse(String projectId, String experimentId, String requestId) {

    public ExperimentDeletionResponse {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }
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
   * @param responsible  the responsible person, can be <code>null</code>
   * @since 1.9.0
   */
  record ProjectContacts(ProjectContact investigator, ProjectContact manager,
                         ProjectContact responsible) {

    public ProjectContacts(ProjectContact investigator, ProjectContact manager) {
      this(investigator, manager, null);
    }

    /**
     * {@inheritDoc} Please use {@link #optionalResponsible} to access the responsible safely.
     *
     * @return the project responsible. Might be null.
     * @since 1.10.0
     */
    @Override
    public ProjectContact responsible() {
      return responsible;
    }

    public Optional<ProjectContact> optionalResponsible() {
      return Optional.ofNullable(responsible);
    }

  }

  /**
   * A project contact.
   *
   * @param fullName   the full name of the person
   * @param email      a valid email address for contact
   * @param oidc       the UUID which identifies the contact within the oidcIssuer
   * @param oidcIssuer the oidcIssuer providing the UUID to identify a user if registered
   * @since 1.9.0
   */
  record ProjectContact(String fullName, String email, String oidc, String oidcIssuer) {

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

  record ProjectResponsible(ProjectContact contact) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

  }

  record ProjectManager(ProjectContact contact) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

  }

  record PrincipalInvestigator(ProjectContact contact) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

  }

  record ProjectInformation(String projectId, ProjectDesign projectDesign, ProjectContacts contacts,
                            FundingInformation fundingInformation, List<String> experimentIds) {

    public ProjectInformation {
      requireNonNull(projectId);
      requireNonNull(projectDesign);
      requireNonNull(contacts);
      requireNonNull(fundingInformation);
      requireNonNull(experimentIds);
    }
  }

  /**
   * Requests project information for a given project identifier.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId the project ID of the project the get the information for.
   * @return a {@link Mono<ProjectInformation>} publishing a {@link ProjectInformation} object on
   * success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ProjectInformation> getProject(String projectId);


  /**
   * Tries to resolve the {@link ProjectCode} for a given project ID.
   *
   * @param projectId the technical id of the project
   * @return the natural id of the project, the {@link ProjectCode} which is known to the user.
   * @throws RequestFailedException if the request was not successfully executed
   * @throws AccessDeniedException  if the user has insufficient rights
   * @since 1.11.0
   */
  Mono<ProjectCode> getProjectCode(String projectId);

  /**
   * The natural id of the project known to the user. Follows the pattern starting with
   * <code>^Q2[A-Z0-9]{4}$</code>.
   *
   * @param value
   * @since 1.11.0
   */
  record ProjectCode(String value) {

    public ProjectCode {
      Objects.requireNonNull(value);
    }
  }

  /**
   * Submits a project creation request and returns a {@link Mono<ProjectCreationResponse>}
   * immediately.
   * <p>
   * This implementation must be non-blocking.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request with information required for project creation.
   * @return {@link Mono<ProjectCreationResponse>} object publishing an
   * {@link ProjectCreationResponse} on success. Exceptions are provided as
   * {@link Mono#error(Throwable)}.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.9.0
   */
  Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException;

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
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request to update a project
   * @return a {@link Mono<ProjectUpdateResponse>} object publishing an
   * {@link ProjectUpdateResponse} on success. Exceptions are provided as
   * {@link Mono#error(Throwable)}.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.9.0
   */
  Mono<ProjectUpdateResponse> update(ProjectUpdateRequest request);

  /**
   * Submits a {@link ProjectDeletionRequest} to remove information from a project.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the actual {@link ProjectDeletionRequest}
   * @return a {@link Mono<ProjectDeletionResponse>} object publishing an
   * {@link ProjectDeletionResponse} on success. Exceptions are provided as
   * {@link Mono#error(Throwable)}
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ProjectDeletionResponse> delete(ProjectDeletionRequest request);

  /**
   * A service request to create funding information for a project.
   *
   * @param request the request with information required for funding information creation.
   * @return a {@link Mono<FundingInformationCreationResponse>} object publishing an
   * {@link FundingInformationCreationResponse}
   * @since
   */
  Mono<FundingInformationCreationResponse> create(FundingInformationCreationRequest request);

  /**
   * Submits a funding information deletion request and returns a reactive
   * {@link Mono<FundingInformationDeletionResponse>} publishing a
   * {@link FundingInformationDeletionResponse}.
   *
   * @param request the request with information required for funding information deletion.
   * @return a {@link Mono<FundingInformationDeletionResponse>} object publishing a
   * {@link FundingInformationDeletionResponse}.
   * @since 1.10.0
   */
  Mono<FundingInformationDeletionResponse> delete(FundingInformationDeletionRequest request);

  /**
   * Submits a project-responsible person creation request and returns a reactive
   * {@link Mono<ProjectResponsibleCreationResponse>}.>}.
   *
   * @param request the request with information required for the responsible person creation.
   * @return a {@link Mono<ProjectResponsibleCreationResponse>} object publishing a
   * {@link ProjectResponsibleCreationResponse}.
   * @since 1.10.0
   */
  Mono<ProjectResponsibleCreationResponse> create(ProjectResponsibleCreationRequest request);

  /**
   * Submits a project-responsible person deletion request and returns a reactive
   * {@link Mono<ProjectResponsibleDeletionResponse>}.
   *
   * @param request the request with information required for the responsible person deletion.
   * @return a {@link Mono<ProjectResponsibleDeletionResponse>} object publishing a
   * {@link ProjectResponsibleDeletionResponse}.
   * @since 1.10.0
   */
  Mono<ProjectResponsibleDeletionResponse> delete(ProjectResponsibleDeletionRequest request);
  //</editor-fold>

  //<editor-fold desc="experiment-related API">

  /**
   * Contains information on one experimental variables
   *
   * @param name   the name of the variable
   * @param levels possible levels of the variable
   * @param unit   the unit of the experimental variable. Can be null if no unit is set
   * @since 1.9.0
   */
  record ExperimentalVariable(String name, List<String> levels, @Nullable String unit) {

    public ExperimentalVariable(String name, List<String> levels) {
      this(name, levels, null);
    }

    public ExperimentalVariable {
      levels = List.copyOf(levels);
    }

    @Override
    public List<String> levels() {
      return List.copyOf(levels);
    }

    public Optional<String> optionalUnit() {
      return Optional.ofNullable(unit);
    }
  }

  /**
   * A level of an experimental variable
   *
   * @param variableName the name of the variable
   * @param levelValue   the value of the level
   * @param unit         the unit for the value of the level. Can be null if no unit is set
   * @since 1.9.0
   */
  record VariableLevel(String variableName, String levelValue,
                       @Nullable String unit) {

  }

  /**
   * Information about an experimental group
   *
   * @param id         the technical identifier of the group
   * @param groupId    the group id within the experiment
   * @param name       the name of the experimental group can be empty but is not expected to be
   *                   null
   * @param sampleSize the number of samples in this experimental group
   * @param levels     the experimental variable levels making up the condition for the samples in
   *                   this group.
   * @since 1.9.0
   */
  record ExperimentalGroup(@Nullable Long id, @Nullable Integer groupId, String name,
                           int sampleSize,
                           List<VariableLevel> levels) {

    public ExperimentalGroup {
      requireNonNull(levels);
      levels = List.copyOf(levels);
    }

    public List<VariableLevel> levels() {
      return List.copyOf(levels);
    }
  }

  /**
   * A container for experimental groups. Can be used in {@link #update(ExperimentUpdateRequest)}
   *
   * @param experimentalGroups the list of experimental groups
   * @since 1.9.0
   */
  record ExperimentalGroups(List<ExperimentalGroup> experimentalGroups) {

    public ExperimentalGroups {
      requireNonNull(experimentalGroups);
      experimentalGroups = List.copyOf(experimentalGroups);
    }
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
  record ExperimentDescription(String experimentName, Set<OntologyTerm> species,
                               Set<OntologyTerm> specimen,
                               Set<OntologyTerm> analytes) implements ExperimentUpdateRequestBody,
      ExperimentUpdateResponseBody {

    public ExperimentDescription {
      species = Set.copyOf(species);
      specimen = Set.copyOf(specimen);
      analytes = Set.copyOf(analytes);
    }
  }

  /**
   * A service request to create a new experimental group.
   *
   * @param projectId    the project's identifier. The project containing the experiment.
   * @param experimentId the experiment's identifier
   * @param group        the experimental group to create
   * @param requestId    the request ID. Needs to be provided by the client and will be referenced
   *                     in the response.
   * @since 1.10.0
   */
  record ExperimentalGroupCreationRequest(String projectId, String experimentId,
                                          ExperimentalGroup group, String requestId) implements
      CacheableRequest {

    public ExperimentalGroupCreationRequest(String projectId, String experimentId,
        ExperimentalGroup group) {
      this(projectId, experimentId, group, UUID.randomUUID().toString());
    }

    public ExperimentalGroupCreationRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }
  }

  /**
   * A service response from a {@link ExperimentalGroupCreationRequest}.
   *
   * @param experimentId the experiment's identifier
   * @param group        the experimental group created
   * @param requestId    the identifier of the original request to which this is a response.
   * @since 1.10.0
   */
  record ExperimentalGroupCreationResponse(String experimentId, ExperimentalGroup group,
                                           String requestId) {

    public ExperimentalGroupCreationResponse {
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }
  }

  /**
   * A service request to update an experimental group.
   *
   * @param projectId    the project's identifier. The project containing the experiment.
   * @param experimentId the experiment's identifier
   * @param group        the experimental group to update
   * @param requestId    the request ID. Needs to be provided by the client and will be referenced
   *                     in the response.
   * @since 1.10.0
   */
  record ExperimentalGroupUpdateRequest(String projectId, String experimentId,
                                        ExperimentalGroup group, String requestId) implements
      CacheableRequest {

    public ExperimentalGroupUpdateRequest(String projectId, String experimentId,
        ExperimentalGroup group) {
      this(projectId, experimentId, group, UUID.randomUUID().toString());
    }

    public ExperimentalGroupUpdateRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }
  }

  /**
   * A service response from a {@link ExperimentalGroupUpdateRequest}.
   *
   * @param experimentId the experiment's identifier
   * @param group        the experimental group updated
   * @param requestId    the identifier of the original request to which this is a response.
   * @since 1.10.0
   */
  record ExperimentalGroupUpdateResponse(String experimentId, ExperimentalGroup group,
                                         String requestId) {

    public ExperimentalGroupUpdateResponse {
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }
  }

  /**
   * A service request to delete an experimental group.
   *
   * @param projectId               the project's identifier. The project containing the
   *                                experiment.
   * @param experimentId            the experiment's identifier'
   * @param experimentalGroupNumber the identifier of the experimental group to delete
   * @param requestId               the request ID. Needs to be provided by the client and will be
   *                                referenced in the response.
   * @since 1.10.0
   */
  record ExperimentalGroupDeletionRequest(String projectId, String experimentId,
                                          Integer experimentalGroupNumber,
                                          String requestId) implements
      CacheableRequest {

    public ExperimentalGroupDeletionRequest(String projectId, String experimentId,
        Integer experimentGroupNumber) {
      this(projectId, experimentId, experimentGroupNumber, UUID.randomUUID().toString());
    }

    public ExperimentalGroupDeletionRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(experimentalGroupNumber);
      requireNonNull(requestId);
    }
  }

  /**
   * A service response from a {@link ExperimentalGroupDeletionRequest}.
   *
   * @param experimentId      the experiment's identifier
   * @param experimentGroupId the identifier of the experimental group deleted
   * @param requestId         the identifier of the original request to which this is a response.
   * @since 1.10.0
   */
  record ExperimentalGroupDeletionResponse(String experimentId, Integer experimentGroupId,
                                           String requestId) {

    public ExperimentalGroupDeletionResponse {
      requireNonNull(experimentId);
      requireNonNull(experimentGroupId);
      requireNonNull(requestId);
    }
  }

  record ExperimentalVariablesCreationResponse(String projectId,
                                               List<ExperimentalVariable> experimentalVariables,
                                               String requestId) {

    public ExperimentalVariablesCreationResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
      experimentalVariables = List.copyOf(experimentalVariables);
    }
  }

  record ExperimentalVariablesCreationRequest(String projectId, String experimentId,
                                              List<ExperimentalVariable> experimentalVariables,
                                              String requestId) {

    public ExperimentalVariablesCreationRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(experimentalVariables);
      requireNonNull(requestId);
      experimentalVariables = List.copyOf(experimentalVariables);
    }

    public ExperimentalVariablesCreationRequest(String projectId, String experimentId,
        List<ExperimentalVariable> experimentalVariables) {
      this(projectId, experimentId, experimentalVariables, UUID.randomUUID().toString());
    }
  }

  record ExperimentalVariablesUpdateRequest(String projectId, String experimentId,
                                            List<ExperimentalVariable> experimentalVariables,
                                            String requestId) implements CacheableRequest {

    public ExperimentalVariablesUpdateRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(experimentalVariables);
      requireNonNull(requestId);
      experimentalVariables = List.copyOf(experimentalVariables);
    }

    public ExperimentalVariablesUpdateRequest(String projectId, String experimentId,
        List<ExperimentalVariable> experimentalVariables) {
      this(projectId, experimentId, experimentalVariables, UUID.randomUUID().toString());
    }
  }

  record ExperimentalVariablesUpdateResponse(String projectId,
                                             List<ExperimentalVariable> experimentalVariables,
                                             String requestId) {

    public ExperimentalVariablesUpdateResponse {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(experimentalVariables);
      experimentalVariables = List.copyOf(experimentalVariables);
    }
  }

  record ExperimentalVariablesDeletionRequest(String projectId, String experimentId,
                                              String requestId) implements CacheableRequest {

    public ExperimentalVariablesDeletionRequest {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }

    public ExperimentalVariablesDeletionRequest(String projectId, String experimentId) {
      this(projectId, experimentId, UUID.randomUUID().toString());
    }
  }

  record ExperimentalVariablesDeletionResponse(String projectId, String experimentId,
                                               String requestId) {

    public ExperimentalVariablesDeletionResponse {
      requireNonNull(projectId);
      requireNonNull(experimentId);
      requireNonNull(requestId);
    }
  }

  /**
   * Return a reactive stream of {@link ExperimentDescription} for a given project.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId the identifier of the project to get the experiments for
   * @return a {@link Flux} of {@link ExperimentDescription}. Exceptions are provided as
   * {@link Mono#error(Throwable)}.
   * @throws RequestFailedException in case the request cannot be processed
   * @throws AccessDeniedException  in case of insufficient rights
   * @since 1.10.0
   */
  Flux<ExperimentDescription> getExperiments(String projectId);

  /**
   * Requests the creation of an experiment and returns a reactive
   * {@link Mono<ExperimentCreationResponse>}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request containing information to create the experiment
   * @return a {@link Mono<ExperimentCreationResponse>} object publishing a
   * {@link ExperimentCreationResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentCreationResponse> create(ExperimentCreationRequest request);

  /**
   * Submits an experiment update request and returns a reactive
   * {@link Mono<ExperimentUpdateResponse>} object immediately.
   * <p>
   * The method is non-blocking.
   * <p>
   * The implementing class must ensure to be able to process all implementing classes of the
   * {@link ProjectUpdateRequestBody} interface contained in the request.
   * <p>
   * The implementing class must also ensure to only return responses with classes implementing the
   * {@link ProjectUpdateResponseBody} interface.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request to update a project
   * @return a {@link Mono<ProjectUpdateResponse>} object publishing an
   * {@link ProjectUpdateResponse} on success. Exceptions are provided as
   * {@link Mono#error(Throwable)}.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.9.0
   */
  Mono<ExperimentUpdateResponse> update(ExperimentUpdateRequest request);

  /**
   * Submits an experiment deletion request and returns a reactive
   * {@link Mono<ExperimentDeletionResponse>}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request to delete an experiment for a project.
   * @return a {@link Mono<ExperimentDeletionResponse>} object publishing a
   * {@link ExperimentDeletionResponse}
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentDeletionResponse> delete(ExperimentDeletionRequest request);

  /**
   * Queries all available experimental variables for a given experiment.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId    the project identifier of the project to get the variables from
   * @param experimentId the experiment identifier of the experiment to get the variables from
   * @return a {@link Flux<ExperimentalVariable>} emitting {@link ExperimentalVariable}s for the
   * experiment.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Flux<ExperimentalVariable> getExperimentalVariables(String projectId, String experimentId);

  /**
   * Submits an experimental variable creation request and returns a reactive
   * {@link Mono<ExperimentalVariablesCreationResponse>}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request with information required for the experimental variable creation.
   * @return a {@link Mono<ExperimentalVariablesCreationResponse>} object publishing a
   * {@link ExperimentalVariablesCreationResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentalVariablesCreationResponse> create(ExperimentalVariablesCreationRequest request);

  /**
   * Submits an experimental variable update request and returns a reactive
   * {@link Mono<ExperimentalVariablesUpdateResponse>.}
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request with information required for the experimental variable update
   * @return a {@link Mono<ExperimentalVariablesUpdateResponse>} object publishing a
   * {@link ExperimentalVariablesUpdateResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentalVariablesUpdateResponse> update(ExperimentalVariablesUpdateRequest request);

  /**
   * Submits an experimental variable deletion request and returns a reactive
   * {@link Mono<ExperimentalVariablesDeletionResponse>}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request with information required for the experimental variable deletion
   * @return a {@link Mono<ExperimentalVariablesDeletionResponse>} object publishing a
   * {@link ExperimentalVariablesDeletionResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentalVariablesDeletionResponse> delete(ExperimentalVariablesDeletionRequest request);

  /**
   * Requests a {@link Flux} of all {@link ExperimentalGroup} for a given experiment in a given
   * project.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId    the project ID for the project the experimental groups shall be retrieved
   *                     for
   * @param experimentId the experiment ID for the experiment the experimental groups shall be
   *                     retrieved
   * @return a {@link Flux} of {@link ExperimentalGroup} for the given project and experiment.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Flux<ExperimentalGroup> getExperimentalGroups(String projectId, String experimentId);

  /**
   * Submits an experimental group creation request and returns a reactive
   * {@link Mono<ExperimentalGroupCreationResponse>}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request to create an experimental group for a project
   * @return a {@link Mono<ExperimentalGroupCreationResponse>} object publishing a
   * {@link ExperimentalGroupCreationResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentalGroupCreationResponse> create(ExperimentalGroupCreationRequest request);

  /**
   * Submits an experimental group update request and returns a reactive
   * {@link Mono<ExperimentalGroupUpdateResponse>}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request to update an experimental group for a project
   * @return a {@link Mono<ExperimentalGroupUpdateResponse>} object publishing a
   * {@link ExperimentalGroupUpdateResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentalGroupUpdateResponse> update(ExperimentalGroupUpdateRequest request);


  record MeasurementRegistrationRequest(String projectId,
                                        MeasurementRegistrationRequestBody measurement,
                                        String requestId) implements CacheableRequest {

    public MeasurementRegistrationRequest {
      requireNonNull(projectId);
      requireNonNull(measurement);
      requireNonNull(requestId);
    }

    public MeasurementRegistrationRequest(String projectId,
        MeasurementRegistrationRequestBody measurement) {
      this(projectId, measurement, UUID.randomUUID().toString());
    }

  }

  /**
   * Submits an experimental group deletion request and returns a reactive
   * {@link Mono<ExperimentalGroupDeletionResponse>}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param request the request to delete an experimental group for a project
   * @return a {@link Mono<ExperimentalGroupDeletionResponse>} object publishing a
   * {@link ExperimentalGroupDeletionResponse} on success.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Mono<ExperimentalGroupDeletionResponse> delete(ExperimentalGroupDeletionRequest request);
  //</editor-fold>


  record MeasurementRegistrationResponse(String requestId,
                                         MeasurementRegistrationRequestBody measurement) {

    public MeasurementRegistrationResponse {
      requireNonNull(requestId);
      requireNonNull(measurement);
    }
  }

  /**
   * Submits a measurement registration request for NGS and returns a reactive
   * {@link Flux<MeasurementRegistrationInformationNGS>} with the measurement information as
   * {@link MeasurementRegistrationInformationNGS}.
   *
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Flux#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param requestStream the request with the measurement information as
   *                      {@link MeasurementRegistrationInformationNGS}.
   * @return a {@link Flux<MeasurementRegistrationInformationNGS>} with the measurement information
   * as {@link MeasurementRegistrationInformationNGS}.
   * @since 1.11.0
   */
  Flux<MeasurementRegistrationResponse> create(Flux<MeasurementRegistrationRequest> requestStream);


  Flux<MeasurementUpdateResponse> update(Flux<MeasurementUpdateRequest> requestStream);

  record MeasurementUpdateRequest(String projectId, MeasurementUpdateRequestBody requestBody,
                                  String requestId) implements CacheableRequest {

    public MeasurementUpdateRequest {
      requireNonNull(projectId);
      requireNonNull(requestBody);
      requireNonNull(requestId);
    }

    public MeasurementUpdateRequest(String projectId, MeasurementUpdateRequestBody requestBody) {
      this(projectId, requestBody, UUID.randomUUID().toString());
    }
  }

  record MeasurementUpdateResponse(String requestId, MeasurementUpdateRequestBody requestBody) {

    public MeasurementUpdateResponse {
      requireNonNull(requestId);
      requireNonNull(requestBody);
    }
  }

  sealed interface MeasurementUpdateRequestBody permits MeasurementUpdateInformationNGS,
      MeasurementUpdateInformationPxP {

  }

  /**
   * Returns a reactive stream of a zipped RO-Crate encoded in UTF-8.
   * <p>
   * The content represents a project summary with information about the research project.
   * <p>
   * Currently, the RO-Crate contains three files:
   *
   * <pre>
   *    ro-crate-metadata.json // required by the RO-Crate specification
   *    project-summary.docx // docx version of <a href="https://schema.org/ResearchProject">ResearchProject</a>
   *    project-summary.yml // yaml encoding of <a href="https://schema.org/ResearchProject">ResearchProject</a>
   *  </pre>
   *
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId the project ID for the project the RO-Crate
   * @return a reactive stream of the zipped RO-Crate. Exceptions are provided as
   * {@link Mono#error(Throwable)}.
   * @throws RequestFailedException in case the request cannot be processed
   * @throws AccessDeniedException  in case of insufficient rights
   * @since 1.10.0
   */
  Flux<ByteBuffer> roCrateSummary(String projectId)
      throws RequestFailedException, AccessDeniedException;

  /**
   * Requests {@link SamplePreview} for a given experiment with pagination support.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId    the project ID for the project to get the samples for
   * @param experimentId the experiment ID for which the sample preview shall be retrieved
   * @param offset       the offset from 0 of all available previews the returned previews should
   *                     start
   * @param limit        the maximum number of previews that should be returned
   * @param sortOrders   the sort orders to apply
   * @param filter       the filter to apply
   * @return a reactive stream of {@link SamplePreview} objects in the experiment. Exceptions are
   * provided as {@link Mono#error(Throwable)}.
   * @throws RequestFailedException if the request could not be executed
   * @since 1.10.0
   */
  Flux<SamplePreview> getSamplePreviews(String projectId, String experimentId, int offset,
      int limit, List<SortOrder> sortOrders, String filter);

  /**
   * Requests all {@link Sample} for a given experiment.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId    the project ID for the project to get the samples for
   * @param experimentId the experiment ID for which the samples shall be retrieved
   * @return a reactive stream of {@link Sample} objects. Exceptions are provided as
   * {@link Mono#error(Throwable)}.
   * @throws RequestFailedException in case the request cannot be executed
   * @since 1.10.0
   */
  Flux<Sample> getSamples(String projectId, String experimentId) throws RequestFailedException;

  /**
   * Requests all {@link Sample} for a given batch
   *
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId the project ID for the project to get the samples for
   * @param batchId   the batch ID the samples shall be retrieved for
   * @return a reactive stream of {@link Sample} objects for the given batch. Exceptions are
   * provided as {@link Mono#error(Throwable)}.
   * @throws RequestFailedException in case the request cannot be executed
   * @since 1.10.0
   */
  Flux<Sample> getSamplesForBatch(String projectId, String batchId) throws RequestFailedException;

  /**
   * Finds the sample for a given sample ID.
   * <p>
   * In case no matching sample is found, a {@link Mono#empty()} is returned.
   *
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param projectId the project id to which the sample belongs to
   * @param sampleId  the sample id of the sample to find
   * @return a reactive container of {@link Sample} for the sample matching the sample id. For no
   * matches a {@link Mono#empty()} is returned. Exceptions are provided as
   * {@link Mono#error(Throwable)}.
   * @throws RequestFailedException in case the request cannot be executed
   * @since 1.10.0
   */
  Mono<Sample> findSample(String projectId, String sampleId);

  /**
   * Requests a {@link Flux} of matching {@link OntologyTerm} for a given search value.
   * <p>
   * The implementation must support pagination based on the provided values for offset and limit.
   * <p>
   * Note: it is not guaranteed that taxa will be emitted to the flux. To search for taxa, please
   * use the dedicated method {@link #getTaxa(String, int, int, List)}.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Flux#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param value  the value for searching matching {@link OntologyTerm}
   * @param offset the offset value from 0 for paginated queries
   * @param limit  the maximum number of hits returned in the flux
   * @return a {@link Flux} of {@link OntologyTerm} matching the search value
   * @throws RequestFailedException if the request was not successfully executed
   * @since 1.10.0
   */
  Flux<OntologyTerm> getTerms(String value, int offset, int limit);

  /**
   * Tries to find the exact matching {@link OntologyTerm} for a given {@link Curie}.
   * <p>
   * If no matching {@link OntologyTerm} can be found, the {@link Mono#empty()} will complete
   * without emitting a value.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param value the {@link Curie} of the term to search for
   * @return a {@link Mono} emitting the {@link OntologyTerm} if an exact match was found or else
   * completes empty.
   * @throws RequestFailedException if the request was not successfully executed
   * @since 1.10.0
   */
  Mono<OntologyTerm> getTermWithCurie(Curie value);

  /**
   * Requests a {@link Flux} of {@link OntologyTerm} for a given search value.
   * <p>
   * The implementation must support:
   * <ul>
   *   <li>pagination based on the provided values for offset and limit</li>
   *   <li>taxa as emitted values, and <strong>taxa only</strong></li>
   * </ul>
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Flux#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param value   the value for searching matching {@link OntologyTerm}
   * @param offset  the offset value from 0 for paginated queries
   * @param limit   the maximum number of hits returned in the flux
   * @param sorting a {@link List} of {@link SortOrder}s to sort the results by
   * @return a {@link Flux} of {@link OntologyTerm} matching the search value
   * @throws RequestFailedException if the request was not successfully executed
   * @since 1.10.0
   */
  Flux<OntologyTerm> getTaxa(String value, int offset, int limit, List<SortOrder> sorting);

  /**
   * Tries to find the exact matching {@link OntologyTerm} for a given {@link Curie}.
   * <p>
   * If no matching {@link OntologyTerm} can be found, the {@link Mono#empty()} will complete
   * without emitting a value.
   * <p>
   * Note: the implementation must only search for taxa.
   * <p>
   * <b>Exceptions</b>
   * <p>
   * Exceptions are wrapped as {@link Mono#error(Throwable)} and are one of the types described in
   * the throw section below.
   *
   * @param value the {@link Curie} of the term to search fo
   * @return a {@link Mono} emitting the {@link OntologyTerm} if an exact match was found or else
   * completes empty.
   * @throws RequestFailedException if the request was not successfully executed
   * @since 1.10.0
   */
  Mono<OntologyTerm> getTaxonWithCurie(Curie value);

  /**
   * Submits multiple validation requests in a single service call.
   *
   * @param requests a {@link Flux} providing {@link ValidationRequest}.
   * @return a {@link Flux} of {@link ValidationResponse}, providing the validation results for the
   * submitted requests. Exceptions are provided as {@link Flux#error(Throwable)}.
   * @throws UnknownRequestException if an unknown request has been used in the service call
   * @throws RequestFailedException  if the request was not successfully executed
   * @throws AccessDeniedException   if the user has insufficient rights
   * @since 1.10.0
   */
  Flux<ValidationResponse> validate(Flux<ValidationRequest> requests);

  /**
   * Requests a sample registration template in a desired {@link MimeType}.
   * <p>
   * If the mime type is not supported, a {@link UnsupportedMimeTypeException} will be provided as
   * {@link Mono#error(Throwable)}.
   *
   * @param projectId    the project ID of the project the template should be created for
   * @param experimentId the experiment ID of the experiment the template should be created for
   * @param mimeType     the mime type the digital object should be
   * @return a {@link Mono} with a {@link DigitalObject} providing the requested template
   * @throws AccessDeniedException        if the user has insufficient rights
   * @throws RequestFailedException       if the request cannot be executed
   * @throws UnsupportedMimeTypeException if the service cannot provide the requested
   *                                      {@link MimeType}
   * @since 1.10.0
   */
  Mono<DigitalObject> sampleRegistrationTemplate(String projectId, String experimentId,
      MimeType mimeType);

  /**
   * Requests a sample update template in a desired {@link MimeType}.
   * <p>
   * If the mime type is not supported, a {@link UnsupportedMimeTypeException} will be provided as
   * {@link Mono#error(Throwable)}.
   *
   * @param projectId    the project ID of the project the template should be created for
   * @param experimentId the experiment ID of the experiment the template should be created for
   * @param batchId      the batch ID for which the samples shall be updated
   * @param mimeType     the mime type the digital object should be
   * @return a {@link Mono} with a {@link DigitalObject} providing the requested template
   * @throws AccessDeniedException        if the user has insufficient rights
   * @throws RequestFailedException       if the request cannot be executed
   * @throws UnsupportedMimeTypeException if the service cannot provide the requested
   *                                      {@link MimeType}
   * @since 1.10.0
   */
  Mono<DigitalObject> sampleUpdateTemplate(String projectId, String experimentId,
      String batchId, MimeType mimeType);

  /**
   * Requests sample information in a desired {@link MimeType}.
   * <p>
   * If the mime type is not supported, a {@link UnsupportedMimeTypeException} will be provided as
   * {@link Mono#error(Throwable)}.
   *
   * @param projectId    the project ID of the project the template should be created for
   * @param experimentId the experiment ID of the experiment the template should be created for
   * @param mimeType     the mime type the digital object should be
   * @return a {@link Mono} with a {@link DigitalObject} providing the requested template
   * @throws AccessDeniedException        if the user has insufficient rights
   * @throws RequestFailedException       if the request cannot be executed
   * @throws UnsupportedMimeTypeException if the service cannot provide the requested
   *                                      {@link MimeType}
   * @since 1.10.0
   */
  Mono<DigitalObject> sampleInformationTemplate(String projectId, String experimentId,
      MimeType mimeType);


  /**
   * Provides information about selected NGS measurements for updating purposes in a requested
   * {@link MimeType}.
   *
   * @param projectId      the id of the project the measurements belong to
   * @param measurementIds a {@link List} of ids of the measurements of interest
   * @param mimeType       the desired {@link MimeType} of the {@link DigitalObject}
   * @return a {@link DigitalObject} in the request {@link MimeType} format
   * @since 1.11.0
   */
  Mono<DigitalObject> measurementUpdateNGS(String projectId, List<String> measurementIds,
      MimeType mimeType);

  /**
   * Provides information about selected proteomics measurements for updating purposes in a
   * requested {@link MimeType}.
   *
   * @param projectId      the id of the project the measurements belong to
   * @param measurementIds a {@link List} of ids of the measurements of interest
   * @param mimeType       the desired {@link MimeType} of the {@link DigitalObject}
   * @return a {@link DigitalObject} in the request {@link MimeType} format
   * @since 1.11.0
   */
  Mono<DigitalObject> measurementUpdatePxP(String projectId, List<String> measurementIds,
      MimeType mimeType);

  sealed interface ExperimentUpdateRequestBody permits ConfoundingVariableAdditions,
      ConfoundingVariableDeletions, ConfoundingVariableUpdates, ExperimentDescription {

  }

  sealed interface ExperimentUpdateResponseBody permits ConfoundingVariables,
      ExperimentDescription {

  }

  /**
   * A validation request body for information that shall be validated by the service.
   * <p>
   * Currently, permits:
   *
   * <ul>
   *   <li>{@link SampleRegistrationInformation}</li>
   *   <li>{@link SampleUpdateInformation}</li>
   *   <li>{@link MeasurementRegistrationInformationNGS}</li>
   *   <li>{@link MeasurementRegistrationInformationPxP}</li>
   *   <li>{@link MeasurementUpdateInformationNGS}</li>
   *   <li>{@link MeasurementUpdateInformationPxP}</li>
   * </ul>
   *
   * @since 1.10.0
   */
  sealed interface ValidationRequestBody permits MeasurementRegistrationInformationNGS,
      MeasurementRegistrationInformationPxP, MeasurementUpdateInformationNGS,
      MeasurementUpdateInformationPxP, SampleRegistrationInformation, SampleUpdateInformation {

  }

  /**
   * Cacheable requests provide a unique identifier so cache implementations can unambiguously
   * manage the requests.
   *
   * @since 1.9.0
   */
  sealed interface CacheableRequest permits ExperimentCreationRequest, ExperimentDeletionRequest,
      ExperimentUpdateRequest, ExperimentalGroupCreationRequest, ExperimentalGroupDeletionRequest,
      ExperimentalGroupUpdateRequest, ExperimentalVariablesDeletionRequest,
      ExperimentalVariablesUpdateRequest, FundingInformationCreationRequest,
      MeasurementRegistrationRequest, MeasurementUpdateRequest, ProjectResponsibleCreationRequest,
      ProjectResponsibleDeletionRequest, ProjectUpdateRequest, ValidationRequest {

    /**
     * Returns an ID that is unique to the request.
     *
     * @return the id
     * @since 1.9.0
     */
    String requestId();

  }

  /**
   * A list of confounding variable information for variable addition. Can be used in
   * {@link #update(ExperimentUpdateRequest)}
   *
   * @param confoundingVariables the variable information
   */
  record ConfoundingVariableAdditions(
      List<ConfoundingVariableInformation> confoundingVariables) implements
      ExperimentUpdateRequestBody {

    public ConfoundingVariableAdditions {
      confoundingVariables = List.copyOf(confoundingVariables);
    }

    @Override
    public List<ConfoundingVariableInformation> confoundingVariables() {
      return List.copyOf(confoundingVariables);
    }
  }

  /**
   * A list of confounding variable information for variable update. Can be used in
   * {@link #update(ExperimentUpdateRequest)}
   *
   * @param confoundingVariables the variable information
   */
  record ConfoundingVariableDeletions(
      List<ConfoundingVariableInformation> confoundingVariables) implements
      ExperimentUpdateRequestBody {

    public ConfoundingVariableDeletions {
      confoundingVariables = List.copyOf(confoundingVariables);
    }

    @Override
    public List<ConfoundingVariableInformation> confoundingVariables() {
      return List.copyOf(confoundingVariables);
    }
  }

  /**
   * A list of confounding variable information for variable deletion. Can be used in
   * {@link #update(ExperimentUpdateRequest)}
   *
   * @param confoundingVariables the variable information
   */
  record ConfoundingVariableUpdates(
      List<ConfoundingVariableInformation> confoundingVariables) implements
      ExperimentUpdateRequestBody {

    public ConfoundingVariableUpdates {
      confoundingVariables = List.copyOf(confoundingVariables);
    }

    @Override
    public List<ConfoundingVariableInformation> confoundingVariables() {
      return List.copyOf(confoundingVariables);
    }
  }

  /**
   * A list of confounding variable information.
   *
   * @param confoundingVariables the variable information
   */
  record ConfoundingVariables(List<ConfoundingVariableInformation> confoundingVariables) implements
      ExperimentUpdateResponseBody {

    public ConfoundingVariables {
      confoundingVariables = List.copyOf(confoundingVariables);
    }

    @Override
    public List<ConfoundingVariableInformation> confoundingVariables() {
      return List.copyOf(confoundingVariables);
    }
  }


  /**
   * Represents an ontology term definition with a simple label that can be used to display the term
   * for humans, its assigned OBO identifier and its globally unique identifier.
   *
   * @param label       an {@link OntologyTerm} label for visualization
   * @param description a short description of the term
   * @param oboId       the assigned OBO identifier
   * @param id          the globally unique identifier of the term
   * @param ontologyId  the identifier of the ontology the term belongs to, e.g. <code>ncit</code>
   *                    for the National Cancer Institute Thesaurus (NCIT)
   * @since 1.10.0
   */
  record OntologyTerm(String label, String description, Curie oboId, URI id, String ontologyId) {

    public OntologyTerm {
      requireNonNull(label);
      requireNonNull(oboId);
      requireNonNull(id);
      requireNonNull(ontologyId);
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OntologyTerm that = (OntologyTerm) o;
      return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id);
    }
  }

  /**
   * Represents a CURIE in the format <code>IDSPACE:LOCALID</code>.
   * <p>
   * Example: <code>GO:0008150</code>
   *
   * @param idSpace the id space defined that holds a set of local identifiers unique within a
   *                space
   * @param localId the local id which is unique within the space
   * @since 1.10.0
   */
  record Curie(String idSpace, String localId) {

    public static Curie parse(String value) {
      requireNonNull(value);
      if (value.contains(":")) {
        var parts = value.split(":");
        return new Curie(parts[0], parts[1]);
      }
      throw new IllegalArgumentException("Invalid Curie: " + value);
    }

    public String toString() {
      return idSpace + ":" + localId;
    }
  }


  /**
   * A service request to create one or more new samples for a project.
   *
   * @param projectId the project ID of the project the samples shall be created for
   * @param requests  a collection of {@link SampleRegistrationInformation} items
   * @since 1.10.0
   */
  record SampleRegistrationRequest(String projectId,
                                   Collection<SampleRegistrationInformation> requests,
                                   String requestId) {

    public SampleRegistrationRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requests = List.copyOf(requests);
    }
  }

  /**
   * A service request to update one or more samples in a project.
   *
   * @param projectId the project ID of the project the samples shall be updated in
   * @param requests  a collection for {@link SampleRegistrationInformation} items
   * @since 1.10.0
   */
  record SampleUpdateRequest(String projectId, Collection<SampleRegistrationInformation> requests,
                             String requestId) {

    public SampleUpdateRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requests = List.copyOf(requests);
    }
  }

  /**
   * A container for a sample update request, containing the sample identifier and the updated
   * information.
   *
   * @param sampleId    the sample ID of the sample to update
   * @param information the new information
   * @since 1.10.0
   */
  record SampleUpdate(String sampleId, SampleInformation information) {

  }

  /**
   * A simple container for sample registration information of an individual sample to register.
   *
   * @param sampleName           the sample name
   * @param biologicalReplicate  the biological replicate label given
   * @param condition            the String representation of a condition
   * @param species              the String representation of a species with CURIE
   * @param specimen             the String representation of a specimen with CURIE
   * @param analyte              the String representation of an analyte with CURIE
   * @param analysisMethod       the String representation of the analysis method
   * @param comment              a users comment
   * @param confoundingVariables confounding variables with as a {@link java.util.HashMap}
   *                             representation
   * @since 1.10.0
   */
  record SampleRegistrationInformation(
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      Map<String, String> confoundingVariables
  ) implements ValidationRequestBody {

  }

  /**
   * A simple container for sample update information of an individual sample to register.
   *
   * @param sampleCode           the sample ID that is known to the user
   * @param sampleName           the sample name
   * @param biologicalReplicate  the biological replicate label given
   * @param condition            the String representation of a condition
   * @param species              the String representation of a species with CURIE
   * @param specimen             the String representation of a specimen with CURIE
   * @param analyte              the String representation of an analyte with CURIE
   * @param analysisMethod       the String representation of the analysis method
   * @param comment              a users comment
   * @param confoundingVariables confounding variables with as a {@link java.util.HashMap}
   *                             representation
   * @since 1.10.0
   */
  record SampleUpdateInformation(
      String sampleCode,
      String sampleName,
      String biologicalReplicate,
      String condition,
      String species,
      String specimen,
      String analyte,
      String analysisMethod,
      String comment,
      Map<String, String> confoundingVariables
  ) implements ValidationRequestBody {

  }

  /**
   * Type interface for the registration information of measurements
   *
   * @since 1.11.0
   */
  sealed interface MeasurementRegistrationRequestBody permits MeasurementRegistrationInformationNGS,
      MeasurementRegistrationInformationPxP {

  }


  /**
   * Information container to register an NGS measurement.
   *
   * @param organisationId        the ROR ID of the organization that performed the measurement
   * @param instrumentCURIE       the CURIE of the measurement device used
   * @param facility              the facility within the organization that actually performed the
   *                              measurement
   * @param sequencingReadType    the sequencing read type used
   * @param libraryKit            the library kit used
   * @param flowCell              the flow cell used
   * @param sequencingRunProtocol the sequencing run protocol
   * @param samplePoolGroup       the name of the sample pool
   * @param specificMetadata      specific metadata that differentiates pooled samples as a
   *                              {@link Map}, with the sample ids as keys and the sample-specific
   *                              measurement annotations as values. Will have only one entry if no
   *                              pooling was done. {@link MeasurementSpecificNGS}
   * @param measurementName       a given name for the measurement. For example a local ID or label
   *                              that helps users to connect the created dataset with the
   *                              registered metadata
   * @since 1.10.0
   */
  record MeasurementRegistrationInformationNGS(
      String organisationId, String instrumentCURIE, String facility,
      String sequencingReadType, String libraryKit, String flowCell,
      String sequencingRunProtocol, String samplePoolGroup,
      Map<String, MeasurementSpecificNGS> specificMetadata,
      String measurementName
  ) implements ValidationRequestBody, MeasurementRegistrationRequestBody {

    public MeasurementRegistrationInformationNGS {
      requireNonNull(organisationId);
      requireNonNull(instrumentCURIE);
      requireNonNull(facility);
      requireNonNull(sequencingReadType);
      requireNonNull(libraryKit);
      requireNonNull(flowCell);
      requireNonNull(sequencingRunProtocol);
      requireNonNull(samplePoolGroup);
      requireNonNull(specificMetadata);
      requireNonNull(measurementName);
      specificMetadata = new HashMap<>(specificMetadata);
      requireNonNull(measurementName);
    }


    /**
     * Returns the {@link List} of sample identifiers this measurement refers to.
     *
     * @return the {@link List} of sample identifiers
     * @since 1.10.0
     */
    public List<String> measuredSamples() {
      return List.copyOf(specificMetadata.keySet());
    }

  }

  /**
   * Information container to update an NGS measurement.
   *
   * @param measurementId         the identifier of the measurement
   * @param organisationId        the ROR ID of the organization that performed the measurement
   * @param instrumentCURIE       the CURIE of the measurement device used
   * @param facility              the facility within the organization that actually performed the
   *                              measurement
   * @param sequencingReadType    the sequencing read type used
   * @param libraryKit            the library kit used
   * @param flowCell              the flow cell used
   * @param sequencingRunProtocol the sequencing run protocol
   * @param samplePoolGroup       the name of the sample pool
   * @param specificMetadata      specific metadata that differentiates pooled samples as a
   *                              {@link Map}, with the sample ids as keys and the sample-specific
   *                              measurement annotations as values. Will have only one entry if no
   *                              pooling was done. {@link MeasurementSpecificNGS}
   * @param measurementName       a given name for the measurement. For example a local ID or label
   *                              that helps users to connect the created dataset with the
   *                              registered metadata
   * @since 1.10.0
   */
  record MeasurementUpdateInformationNGS(
      String measurementId,
      String organisationId, String instrumentCURIE,
      String facility,
      String sequencingReadType, String libraryKit,
      String flowCell,
      String sequencingRunProtocol, String samplePoolGroup,
      Map<String, MeasurementSpecificNGS> specificMetadata,
      String measurementName
  ) implements ValidationRequestBody,
      MeasurementUpdateRequestBody {

    public MeasurementUpdateInformationNGS {
      requireNonNull(measurementId);
      requireNonNull(organisationId);
      requireNonNull(instrumentCURIE);
      requireNonNull(facility);
      requireNonNull(sequencingReadType);
      requireNonNull(libraryKit);
      requireNonNull(flowCell);
      requireNonNull(sequencingRunProtocol);
      requireNonNull(samplePoolGroup);
      requireNonNull(specificMetadata);
      specificMetadata = new HashMap<>(specificMetadata);
    }

    /**
     * Returns the {@link List} of sample identifiers this measurement refers to.
     *
     * @return the {@link List} of sample identifiers
     * @since 1.10.0
     */
    public List<String> measuredSamples() {
      return List.copyOf(specificMetadata.keySet());
    }

  }

  /**
   * Metadata that describes measurement properties, that are unique to the sample presented in the
   * measurement (e.g., when pooling was done)
   *
   * @param indexI7 the i7 index used in the measurement to discriminate a sample
   * @param indexI5 the i5 index used in the measurement to discriminate a sample
   * @param comment some comment from the measuring lab
   * @since 1.10.0
   */
  record MeasurementSpecificNGS(
      String indexI7,
      String indexI5,
      String comment
  ) {

  }

  /**
   * Information container to register a proteomics measurement.
   *
   * @param technicalReplicateName the name of the technical replicate
   * @param organisationId         the ROR ID of the organization that performed the measurement
   * @param msDeviceCURIE          the CURIE of the mass spectrometry device used for the
   *                               measurement
   * @param samplePoolGroup        the name of the sample pool
   * @param facility               the name of the facility that performed the measurement
   * @param digestionEnzyme        the enzyme used for proteolytic digestion
   * @param digestionMethod        the digestion method
   * @param enrichmentMethod       the enrichment method used
   * @param injectionVolume        the amount of the analyte injected for the measurement
   * @param lcColumn               the liquid chromatography column used to separate compounds
   * @param lcmsMethod             the method used
   * @param labelingType           the type of the labeling used
   * @param specificMetadata       specific metadata that differentiates pooled samples as a
   *                               {@link Map}, with the sample ids as keys and the sample-specific
   *                               measurement annotations as values. Will have only one entry if no
   *                               pooling was done. {@link MeasurementSpecificPxP}
   * @param measurementName        a given name for the measurement. For example a local ID or label
   *                               that helps users to connect the created dataset with the
   *                               registered metadata
   * @since 1.10.0
   */
  record MeasurementRegistrationInformationPxP(
      String technicalReplicateName,
      String organisationId,
      String msDeviceCURIE,
      String samplePoolGroup,
      String facility,
      String digestionEnzyme,
      String digestionMethod,
      String enrichmentMethod,
      String injectionVolume,
      String lcColumn,
      String lcmsMethod,
      String labelingType,
      Map<String, MeasurementSpecificPxP> specificMetadata,
      String measurementName
  ) implements ValidationRequestBody, MeasurementRegistrationRequestBody {

    /**
     * Returns the {@link List} of sample identifiers this measurement refers to.
     *
     * @return the {@link List} of sample identifiers
     * @since 1.10.0
     */
    public List<String> measuredSamples() {
      return List.copyOf(specificMetadata.keySet());
    }

  }

  /**
   * Information container to register a proteomics measurement.
   *
   * @param measurementId          the identifier of the measurement
   * @param technicalReplicateName the name of the technical replicate
   * @param organisationId         the ROR ID of the organization that performed the measurement
   * @param msDeviceCURIE          the CURIE of the mass spectrometry device used for the
   *                               measurement
   * @param samplePoolGroup        the name of the sample pool
   * @param facility               the name of the facility that performed the measurement
   * @param digestionEnzyme        the enzyme used for proteolytic digestion
   * @param digestionMethod        the digestion method
   * @param enrichmentMethod       the enrichment method used
   * @param injectionVolume        the amount of the analyte injected for the measurement
   * @param lcColumn               the liquid chromatography column used to separate compounds
   * @param lcmsMethod             the method used
   * @param labelingType           the type of the labeling used
   * @param specificMetadata       specific metadata that differentiates pooled samples as a
   *                               {@link Map}, with the sample ids as keys and the sample-specific
   *                               measurement annotations as values. Will have only one entry if no
   *                               pooling was done. {@link MeasurementSpecificPxP}
   * @param measurementName        a given name for the measurement. For example a local ID or label
   *                               that helps users to connect the created dataset with the
   *                               registered metadata
   * @since 1.10.0
   */
  record MeasurementUpdateInformationPxP(
      String measurementId,
      String technicalReplicateName,
      String organisationId,
      String msDeviceCURIE,
      String samplePoolGroup,
      String facility,
      String digestionEnzyme,
      String digestionMethod,
      String enrichmentMethod,
      String injectionVolume,
      String lcColumn,
      String lcmsMethod,
      String labelingType,
      Map<String, MeasurementSpecificPxP> specificMetadata,
      String measurementName
  ) implements ValidationRequestBody, MeasurementUpdateRequestBody {

    /**
     * Returns the {@link List} of sample identifiers this measurement refers to.
     *
     * @return the {@link List} of sample identifiers
     * @since 1.10.0
     */
    public List<String> measuredSamples() {
      return List.copyOf(specificMetadata.keySet());
    }
  }

  /**
   * Metadata that describes a measurement properties, that are unique to the sample presented in
   * the measurement (e.g., when pooling was done)
   *
   * @param label        the label used to discriminate the sample
   * @param fractionName the fraction name
   * @param comment      some comment from the measuring lab
   * @since 1.10.0
   */
  record MeasurementSpecificPxP(
      String label,
      String fractionName,
      String comment
  ) {

  }

  /**
   * Requests all available {@link RawDatasetInformationPxP} for a given experiment.
   * <p>
   * The request supports pagination.
   *
   * @param projectId    the identifier of the project the experiment belongs to
   * @param experimentId the identifier of the experiment to query the measurements for
   * @param offset       the offset value for the search to continue (pagination)
   * @param limit        the maximum number of results to return (pagination)
   * @param sorting      configuration of the property to sort by
   * @param filter       a term used for filtering matching datasets containing the term
   * @return a reactive {@link Flux} of {@link RawDatasetInformationPxP}
   * @since 1.11.0
   */
  Flux<RawDatasetInformationPxP> getRawDatasetInformationPxP(String projectId, String experimentId,
      int offset,
      int limit, SortRawData sorting, String filter);

  /**
   * Requests all available {@link RawDatasetInformationNgs} for a given experiment.
   * <p>
   * The request supports pagination.
   *
   * @param projectId    the identifier of the project the experiment belongs to
   * @param experimentId the identifier of the experiment to query the measurements for
   * @param offset       the offset value for the search to continue (pagination)
   * @param limit        the maximum number of results to return (pagination)
   * @param sorting      configuration of the property to sort by
   * @param filter       a term used for filtering matching datasets containing the term
   * @return a reactive {@link Flux} of {@link RawDatasetInformationNgs}
   * @since 1.11.0
   */
  Flux<RawDatasetInformationNgs> getRawDatasetInformationNgs(String projectId, String experimentId,
      int offset,
      int limit, SortRawData sorting, String filter);

  /**
   * A simple information container for raw data stored for a given measurement.
   *
   * @param measurementId    the measurement identifier for which the raw data is described
   * @param totalSizeBytes   the total size of the raw dataset for a given measurement. The unit is
   *                         in bytes.
   * @param numberOfFiles    the number of files contained in the raw dataset
   * @param fileTypes        the file types contained in the raw dataset
   * @param registrationDate the date of registration
   * @since 1.11.0
   */
  record RawDataset(String measurementId, long totalSizeBytes, int numberOfFiles,
                    Set<String> fileTypes, Instant registrationDate) {

    public RawDataset {
      requireNonNull(measurementId);
      requireNonNull(fileTypes);
      requireNonNull(registrationDate);
    }
  }

  /**
   * Aggregated information of a raw dataset derived from measurement of the NGS domain. Currently
   * consists of the core {@link RawDataset} and all {@link BasicSampleInformation}.
   *
   * @param dataset                 the actual {@link RawDataset}
   * @param linkedSampleInformation further information about the linked samples as
   *                                {@link BasicSampleInformation}.
   * @since 1.11.0
   */
  record RawDatasetInformationNgs(RawDataset dataset,
                                  List<BasicSampleInformation> linkedSampleInformation) {

    public RawDatasetInformationNgs {
      requireNonNull(dataset);
      requireNonNull(linkedSampleInformation);
      linkedSampleInformation = List.copyOf(linkedSampleInformation);
    }
  }

  /**
   * Available properties to sort raw data queries.
   *
   * @since 1.11.0
   */
  enum SortFieldRawData {
    REGISTRATION_DATE
  }

  enum SortDirection {
    ASC, DESC
  }

  record SortRawData(SortFieldRawData sortField, SortDirection sortDirection) {

  }


  /**
   * Aggregated information of a raw dataset derived from measurement of the proteomics domain.
   * Currently consists of the core {@link RawDataset} and all {@link BasicSampleInformation}.
   *
   * @param dataset                 the actual {@link RawDataset}
   * @param linkedSampleInformation further information about the linked samples as
   *                                {@link BasicSampleInformation}.
   * @since 1.11.0
   */
  record RawDatasetInformationPxP(RawDataset dataset,
                                  List<BasicSampleInformation> linkedSampleInformation) {

    public RawDatasetInformationPxP {
      requireNonNull(dataset);
      requireNonNull(linkedSampleInformation);
      linkedSampleInformation = List.copyOf(linkedSampleInformation);
    }
  }

  /**
   * Basic sample information that can be used to enrich other information containers, e.g.
   * {@link RawDatasetInformation}.
   *
   * @param sampleId   unique identifier for a sample
   * @param sampleName the assigned sample name
   * @since 1.11.0
   */
  record BasicSampleInformation(String sampleId, String sampleName) {

  }


  /**
   * The actual request container for metadata validation.
   * <p>
   * A validation request contains a {@link ValidationRequestBody} with the actual metadata to be
   * validated, next to the project ID and the request ID.
   *
   * @param projectId   the project ID of the project the metadata shall be validated for
   * @param requestBody the actual metadata container with the information to be validated
   * @param requestId   the request ID, needs to be provided by the client and will be referenced in
   *                    the response.
   * @since 1.10.0
   */
  record ValidationRequest(String projectId, String experimentId, ValidationRequestBody requestBody,
                           String requestId) implements CacheableRequest {

    public ValidationRequest(String projectId, String experimentId,
        ValidationRequestBody requestBody) {
      this(projectId, experimentId, requestBody, UUID.randomUUID().toString());
    }

    public ValidationRequest {
      requireNonNull(projectId);
      requireNonNull(requestId);
      requireNonNull(requestBody);
      requireNonNull(experimentId);
    }
  }

  /**
   * The response to a corresponding {@link ValidationRequest} with information about the actual
   * validation result.
   * <p>
   * The result itself is provided in the {@link ValidationResult} property.
   *
   * @param requestId the original ID of the request from {@link ValidationRequest}
   * @param result    the validation report provided as {@link ValidationResult}
   * @since 1.10.0
   */
  record ValidationResponse(String requestId, ValidationResult result) {

    public ValidationResponse {
      requireNonNull(requestId);
    }
  }

  /**
   * Exception to indicate that the service did not recognise the request.
   *
   * @since 1.9.0
   */
  class UnknownRequestException extends RuntimeException {

    private String requestId;

    public UnknownRequestException(String message) {
      super(message);
    }

    public UnknownRequestException(String message, String requestId) {
      super(message);
      this.requestId = requestId;
    }

    public String getRequestId() {
      return requestId;
    }
  }

  /**
   * Exception to indicate that the service tried to execute the request, but it failed.
   *
   * @since 1.9.0
   */
  class RequestFailedException extends RuntimeException {

    private String requestId;

    public RequestFailedException(String message) {
      super(message);
    }

    public RequestFailedException(String message, String requestId) {
      super(message);
      this.requestId = requestId;
    }

    public RequestFailedException(String message, Throwable cause) {
      super(message, cause);
    }

    public RequestFailedException(String message, Throwable cause, String requestId) {
      super(message, cause);
      this.requestId = requestId;
    }

    public String getRequestId() {
      return requestId;
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

  /**
   * Exception to indicate that a service implementation cannot handle a certain mime type.
   *
   * @since 1.10.0
   */
  class UnsupportedMimeTypeException extends RuntimeException {

    public UnsupportedMimeTypeException(String message) {
      super(message);
    }
  }

}
