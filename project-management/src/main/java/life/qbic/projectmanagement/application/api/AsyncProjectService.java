package life.qbic.projectmanagement.application.api;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.batch.SampleUpdateRequest.SampleInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.measurement.Labeling;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
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

  /*
  API method section - start
   */

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


  Mono<ExperimentDeletionResponse> delete(ExperimentDeletionRequest request);

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
   * use the dedicated method {@link #searchTaxa(String, int, int)}.
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
  Flux<OntologyTerm> searchTerm(String value, int offset, int limit);

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
   * @param value the {@link Curie} of the term to search for
   * @return a {@link Mono} emitting the {@link OntologyTerm} if an exact match was found or else
   * completes empty.
   * @throws RequestFailedException if the request was not successfully executed
   * @since 1.10.0
   */
  Mono<OntologyTerm> searchByCurie(Curie value);

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
   * @param value  the value for searching matching {@link OntologyTerm}
   * @param offset the offset value from 0 for paginated queries
   * @param limit  the maximum number of hits returned in the flux
   * @return a {@link Flux} of {@link OntologyTerm} matching the search value
   * @throws RequestFailedException if the request was not successfully executed
   * @since 1.10.0
   */
  Flux<OntologyTerm> searchTaxa(String value, int offset, int limit);

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
   * @param value the {@link Curie} of the term to search fo
   * @return a {@link Mono} emitting the {@link OntologyTerm} if an exact match was found or else
   * completes empty.
   * @throws RequestFailedException if the request was not successfully executed
   * @since 1.10.0
   */
  Mono<OntologyTerm> searchTaxonByCurie(Curie value);

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


  /*
  API method section - end
   */


  /*
  API concept section - start
   */

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

  sealed interface ExperimentUpdateRequestBody permits ConfoundingVariableAdditions,
      ConfoundingVariableDeletions, ConfoundingVariableUpdates, ExperimentDescription,
      ExperimentalGroups, ExperimentalVariableAdditions {

  }

  sealed interface ExperimentUpdateResponseBody permits ConfoundingVariables, ExperimentDescription,
      ExperimentalGroups, ExperimentalVariables {

  }

  /**
   * A validation request body for information that shall be validated by the service.
   * <p>
   * Currently, permits:
   *
   * <ul>
   *   <li>{@link SampleRegistrationInformation}</li>
   *   <li>{@link SampleUpdateInformation}</li>
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
  sealed interface CacheableRequest permits ExperimentUpdateRequest, ProjectUpdateRequest,
      ValidationRequest {

    /**
     * Returns an ID that is unique to the request.
     *
     * @return the id
     * @since 1.9.0
     */
    String requestId();

  }

  sealed interface ProjectDeletionRequestBody permits FundingDeletion,
      ProjectResponsibleDeletion {

  }

  sealed interface ExperimentDeletionRequestBody permits ExperimentalVariableDeletions {

  }


  sealed interface ExperimentDeletionResponseBody permits ExperimentalVariables {

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

  record ProjectResponsible(ProjectContact contact) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

  }

  record ProjectManager(ProjectContact contact) implements ProjectUpdateRequestBody,
      ProjectUpdateResponseBody {

  }

  record PrincipalInvestigator(ProjectContact contact) implements ProjectUpdateRequestBody,
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

    public ExperimentalVariable(String name, Set<String> levels) {
      this(name, levels, "");
    }

    public ExperimentalVariable {
      levels = Set.copyOf(levels);
    }

    @Override
    public Set<String> levels() {
      return Set.copyOf(levels);
    }
  }

  /**
   * Information about variables that should be created
   *
   * @param experimentalVariables
   * @since 1.9.2
   */
  record ExperimentalVariableAdditions(List<ExperimentalVariable> experimentalVariables) implements
      ExperimentUpdateRequestBody {

    public ExperimentalVariableAdditions {
      experimentalVariables = List.copyOf(experimentalVariables);
    }

    @Override
    public List<ExperimentalVariable> experimentalVariables() {
      return List.copyOf(experimentalVariables);
    }
  }

  /**
   * Information about variables that should be deleted
   *
   * @param experimentalVariables
   */
  record ExperimentalVariableDeletions(List<ExperimentalVariable> experimentalVariables) implements
      ExperimentDeletionRequestBody {

    public ExperimentalVariableDeletions {
      experimentalVariables = List.copyOf(experimentalVariables);
    }

    @Override
    public List<ExperimentalVariable> experimentalVariables() {
      return List.copyOf(experimentalVariables);
    }
  }

  /**
   * Container of experimental variables. Can be used in {@link #update(ExperimentUpdateRequest)}.
   *
   * @param experimentalVariables the list of experimental variables
   * @since 1.9.0
   */
  record ExperimentalVariables(List<ExperimentalVariable> experimentalVariables) implements
      ExperimentUpdateResponseBody, ExperimentDeletionResponseBody {


    public ExperimentalVariables {
      experimentalVariables = List.copyOf(experimentalVariables);
    }

    @Override
    public List<ExperimentalVariable> experimentalVariables() {
      return List.copyOf(experimentalVariables);
    }
  }

  /**
   * A level of an experimental variable
   *
   * @param variableId   the identifier of the variable
   * @param variableName the name of the variable
   * @param levelValue   the value of the level
   * @param unit         the unit for the value of the level. Can be null if no unit is set
   * @since 1.9.0
   */
  record VariableLevel(Long variableId, String variableName, String levelValue,
                       @Nullable String unit) {

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

    public ExperimentalGroup {
      levels = Set.copyOf(levels);
    }
  }

  /**
   * A container for experimental groups. Can be used in {@link #update(ExperimentUpdateRequest)}
   *
   * @param experimentalGroups the list of experimental groups
   * @since 1.9.0
   */
  record ExperimentalGroups(List<ExperimentalGroup> experimentalGroups) implements
      ExperimentUpdateRequestBody, ExperimentUpdateResponseBody {

    public ExperimentalGroups {
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
   * A service request to update an experiment
   *
   * @param projectId    the project's identifier. The project containing the experiment.
   * @param experimentId the experiment's identifier
   * @param body         the request body containing information on what was updated
   * @param requestId    the request ID, needs to be provided by the client and will be referenced
   *                     in the response. If <code>null</code> or {@link String#isBlank()} is true,
   *                     then a random UUID is assigned with {@link UUID#randomUUID()}
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
   * Represents an ontology term definition with a simple label that can be used to display the term
   * for humans, its assigned OBO identifier and its globally unique identifier.
   *
   * @param label an {@link OntologyTerm} label for visualisation
   * @param oboId the assigned OBO identifier
   * @param id    the globally unique identifier of the term
   * @since 1.10.0
   */
  record OntologyTerm(String label, Curie oboId, URI id) {

    public OntologyTerm {
      requireNonNull(oboId);
      requireNonNull(id);
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

  }

  /**
   * A service request to create one or more new samples for a project.
   *
   * @param projectId the project ID of the project the samples shall be created for
   * @param requests  a collection of {@link SampleRegistrationInformation} items
   * @since 1.10.0
   */
  record SampleRegistrationRequest(String projectId,
                                   Collection<SampleRegistrationInformation> requests) {

    public SampleRegistrationRequest(String projectId,
        Collection<SampleRegistrationInformation> requests) {
      this.projectId = projectId;
      this.requests = List.copyOf(requests);
    }
  }

  /**
   * A service request to update one or more samples in a project.
   *
   * @param projectId the project ID of the project the samples shall be updated in
   * @param requests  a collection for {@link SampleRegistrationInformation} items
   * @since 1.10.0
   */
  record SampleUpdateRequest(String projectId, Collection<SampleRegistrationInformation> requests) {

    public SampleUpdateRequest(String projectId,
        Collection<SampleRegistrationInformation> requests) {
      this.projectId = projectId;
      this.requests = List.copyOf(requests);
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
   * @param experimentId         the experiment ID of the experiment the sample should be registered
   *                             to
   * @param projectId            the project ID of the project the experiment belongs to
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
      Map<String, String> confoundingVariables,
      String experimentId,
      String projectId
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
   * @param experimentId         the experiment ID of the experiment the sample should be registered
   *                             to
   * @param projectId            the project ID of the project the experiment belongs to
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
      Map<String, String> confoundingVariables,
      String experimentId,
      String projectId
  ) implements ValidationRequestBody {

  }


  record MeasurementRegistrationInformationNGS(
      Collection<String> sampleCodes,
      String organisationId, String instrumentCURI, String facility,
      String sequencingReadType, String libraryKit, String flowCell,
      String sequencingRunProtocol, String samplePoolGroup,
      String indexI7, String indexI5,
      String comment
  ) implements ValidationRequestBody {

  }

  record MeasurementUpdateInformationNGS(
      String measurementCode,
      Collection<String> sampleCodes,
      String organisationId, String instrumentCURI,
      String facility,
      String sequencingReadType, String libraryKit,
      String flowCell,
      String sequencingRunProtocol, String samplePoolGroup,
      String indexI7, String indexI5,
      String comment) implements ValidationRequestBody {

  }

  record MeasurementRegistrationInformationPxP(
      SampleCode sampleCode,
      String technicalReplicateName,
      String organisationId,
      String msDeviceCURIE,
      String samplePoolGroup,
      String facility,
      String fractionName,
      String digestionEnzyme,
      String digestionMethod,
      String enrichmentMethod,
      String injectionVolume,
      String lcColumn,
      String lcmsMethod,
      Labeling labeling,
      String comment
  ) implements ValidationRequestBody {

  }

  record MeasurementUpdateInformationPxP(
      String measurementId,
      SampleCode sampleCode,
      String technicalReplicateName,
      String organisationId,
      String msDeviceCURIE,
      String samplePoolGroup,
      String facility,
      String fractionName,
      String digestionEnzyme,
      String digestionMethod,
      String enrichmentMethod,
      String injectionVolume,
      String lcColumn,
      String lcmsMethod,
      Labeling labeling,
      String comment
  ) implements ValidationRequestBody {

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
   * @param requestId   the request ID, needs to be provided by the client and will be referenced in
   *                    the response. If <code>null</code> or {@link String#isBlank()} is true, then
   *                    a random UUID is assigned with {@link UUID#randomUUID()}.
   * @since 1.9.0
   */
  record ProjectUpdateRequest(String projectId, ProjectUpdateRequestBody requestBody,
                              String requestId) implements CacheableRequest {

    public ProjectUpdateRequest {
      if (projectId == null) {
        throw new IllegalArgumentException("Project ID cannot be null");
      }
      if (projectId.isBlank()) {
        throw new IllegalArgumentException("Project ID cannot be blank");
      }
      if (requestId == null || requestId.isBlank()) {
        requestId = UUID.randomUUID().toString();
      }
    }

    public ProjectUpdateRequest(String projectId, ProjectUpdateRequestBody requestBody) {
      this(projectId, requestBody, UUID.randomUUID().toString());
    }

  }

  record ProjectDeletionRequest(String projectId, String requestId,
                                ProjectDeletionRequestBody body) {

    public ProjectDeletionRequest {
      if (projectId == null) {
        throw new IllegalArgumentException("Project ID cannot be null");
      }
      if (projectId.isBlank()) {
        throw new IllegalArgumentException("Project ID cannot be blank");
      }
      if (requestId == null || requestId.isBlank()) {
        requestId = UUID.randomUUID().toString();
      }
    }

    public ProjectDeletionRequest(String projectId, ProjectDeletionRequestBody requestBody) {
      this(projectId, UUID.randomUUID().toString(), requestBody);
    }
  }

  record ProjectDeletionResponse(String projectId, String requestId) {

  }

  record FundingDeletion() implements ProjectDeletionRequestBody {

  }

  record ProjectResponsibleDeletion() implements ProjectDeletionRequestBody {

  }

  /**
   * A service response from an update project information request.
   *
   * @param projectId    the project's id
   * @param responseBody the information that was updated.
   * @param requestId    the request ID, needs to be provided by the client and will be referenced
   *                     in the response. If <code>null</code> or {@link String#isBlank()} is true,
   *                     then a random UUID is assigned with {@link UUID#randomUUID()}
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
      if (requestId == null || requestId.isBlank()) {
        requestId = UUID.randomUUID().toString();
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

    boolean hasRequestId() {
      return nonNull(requestId);
    }

  }

  record ExperimentDeletionRequest(String projectId, String experimentId, String requestId,
                                   ExperimentDeletionRequestBody body) {

    public ExperimentDeletionRequest {
      if (projectId == null) {
        throw new IllegalArgumentException("Project ID cannot be null");
      }
      if (projectId.isBlank()) {
        throw new IllegalArgumentException("Project ID cannot be blank");
      }
      if (experimentId == null || experimentId.isBlank()) {
        throw new IllegalArgumentException("Experiment ID cannot be empty");
      }
      if (requestId == null || requestId.isBlank()) {
        requestId = UUID.randomUUID().toString();
      }
    }

    public ExperimentDeletionRequest(String projectId, String experimentId,
        ExperimentDeletionRequestBody body) {
      this(projectId, experimentId, null, body);
    }
  }

  record ExperimentDeletionResponse(String projectId, String experimentId, String requestId,
                                    ExperimentDeletionResponseBody body) {

    public ExperimentDeletionResponse {
      if (projectId == null) {
        throw new IllegalArgumentException("Project ID cannot be null");
      }
      if (projectId.isBlank()) {
        throw new IllegalArgumentException("Project ID cannot be blank");
      }
      if (experimentId == null || experimentId.isBlank()) {
        throw new IllegalArgumentException("Experiment ID cannot be empty");
      }
      if (requestId == null || requestId.isBlank()) {
        throw new IllegalArgumentException("Request information cannot be empty");
      }
    }
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
   *                    the response. If <code>null</code> or {@link String#isBlank()} is true, then
   *                    a random UUID is assigned with {@link UUID#randomUUID()}
   * @since 1.10.0
   */
  record ValidationRequest(String projectId, ValidationRequestBody requestBody,
                           String requestId) implements CacheableRequest {

    public ValidationRequest {
      if (projectId == null) {
        throw new IllegalArgumentException("Project ID cannot be null");
      }
      if (projectId.isBlank()) {
        throw new IllegalArgumentException("Project ID cannot be blank");
      }
      if (requestId == null || requestId.isBlank()) {
        requestId = UUID.randomUUID().toString();
      }
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
      if (requestId == null || requestId.isBlank()) {
        throw new IllegalArgumentException("Request ID cannot be null or blank");
      }
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
