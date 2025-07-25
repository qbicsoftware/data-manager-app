package life.qbic.projectmanagement.application.api;

import static java.util.Objects.nonNull;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContextMany;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.reactiveSecurity;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ValidationResult;
import life.qbic.projectmanagement.application.VirtualThreadScheduler;
import life.qbic.projectmanagement.application.api.fair.ContactPoint;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.fair.DigitalObjectFactory;
import life.qbic.projectmanagement.application.api.fair.ResearchProject;
import life.qbic.projectmanagement.application.api.template.TemplateService;
import life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.ontology.OntologyClass;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository.ProjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.FluxSink.OverflowStrategy;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

/**
 * Implementation of the {@link AsyncProjectService} interface.
 * <p>
 * This is the class that should make the actual individual service orchestration and gets all
 * services injected.
 *
 * @since 1.9.0
 */
@Service
public class AsyncProjectServiceImpl implements AsyncProjectService {

  private static final String ACCESS_DENIED = "Access denied";
  private static final Logger log = LoggerFactory.logger(AsyncProjectServiceImpl.class);
  private final ProjectInformationService projectService;
  private final Scheduler scheduler;
  private final SampleInformationService sampleInfoService;
  private final DigitalObjectFactory digitalObjectFactory;
  private final TemplateService templateService;
  private final SampleValidationService sampleValidationService;
  private final MeasurementValidationService measurementValidationService;
  private final ExperimentInformationService experimentInformationService;
  private final TerminologyService terminologyService;
  private final SpeciesLookupService taxaService;

  public AsyncProjectServiceImpl(
      @Autowired ProjectInformationService projectService,
      @Autowired SampleInformationService sampleInfoService,
      @Autowired Scheduler scheduler,
      @Autowired DigitalObjectFactory digitalObjectFactory,
      @Autowired TemplateService templateService,
      @Autowired SampleValidationService sampleValidationService,
      @Autowired MeasurementValidationService measurementValidationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired TerminologyService termService,
      @Autowired SpeciesLookupService taxaService) {
    this.projectService = Objects.requireNonNull(projectService);
    this.sampleInfoService = Objects.requireNonNull(sampleInfoService);
    this.scheduler = Objects.requireNonNull(scheduler);
    this.digitalObjectFactory = Objects.requireNonNull(digitalObjectFactory);
    this.templateService = Objects.requireNonNull(templateService);
    this.sampleValidationService = Objects.requireNonNull(sampleValidationService);
    this.measurementValidationService = Objects.requireNonNull(measurementValidationService);
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.terminologyService = Objects.requireNonNull(termService);
    this.taxaService = Objects.requireNonNull(taxaService);
  }

  private static Retry defaultRetryStrategy() {
    return Retry.maxInARow(5)
        .doBeforeRetry(retrySignal -> log.warn("Operation failed (" + retrySignal + ")"));
  }

  private static Set<OntologyTerm> convertToApi(
      Collection<life.qbic.projectmanagement.domain.model.OntologyTerm> terms) {
    return terms.stream().map(AsyncProjectServiceImpl::convertToApi).collect(Collectors.toSet());
  }

  private static OntologyTerm convertToApi(
      life.qbic.projectmanagement.domain.model.OntologyTerm term) {
    return new OntologyTerm(term.getLabel(), term.getDescription(),
        Curie.parse(term.oboId().toString()),
        URI.create(term.getClassIri()), term.getOntologyAbbreviation());
  }

  private static OntologyTerm convertToApi(OntologyClass term) {
    return new OntologyTerm(term.getClassLabel(), term.getDescription(),
        Curie.parse(term.oboId()), URI.create(term.getClassIri()),
        term.getOntologyAbbreviation());
  }

  private static ExperimentInformationService.VariableLevel convertFromApi(VariableLevel level) {
    return new ExperimentInformationService.VariableLevel(level.variableName(), level.levelValue(),
        level.unit());
  }

  private static Throwable mapToAPIException(Throwable e, String message) {
    if (e instanceof org.springframework.security.access.AccessDeniedException) {
      return new AccessDeniedException(ACCESS_DENIED);
    }
    return new RequestFailedException(message, e);

  }

  @Override
  public Mono<ExperimentCreationResponse> create(ExperimentCreationRequest request) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Mono<ExperimentalGroupCreationResponse> create(ExperimentalGroupCreationRequest request) {
    var call = Mono.fromCallable(() -> {
      var group = request.group();
      var createdGroup = experimentInformationService.createExperimentalGroup(request.projectId(),
          ExperimentId.parse(request.experimentId()),
          new ExperimentInformationService.ExperimentalGroup(null, null, group.name(),
              group.levels().stream().map(AsyncProjectServiceImpl::convertFromApi).toList(),
              group.sampleSize()));
      return new ExperimentalGroupCreationResponse(request.experimentId(),
          new ExperimentalGroup(createdGroup.id(), createdGroup.groupNumber(), createdGroup.name(),
              createdGroup.replicateCount(), createdGroup.levels().stream()
              .map(this::convertLevelToApi)
              .toList()), request.requestId());
    });

    String errorMessage = "Error creating experimental group";
    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .retryWhen(defaultRetryStrategy())
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e -> mapToAPIException(e, errorMessage));
  }

  private VariableLevel convertLevelToApi(ExperimentInformationService.VariableLevel level) {
    return new VariableLevel(level.variableName(), level.variableName(), level.unit());
  }

  @Override
  public Mono<ExperimentalGroupUpdateResponse> update(ExperimentalGroupUpdateRequest request) {
    var call = Mono.fromCallable(() -> {
      if (sampleInfoService.hasSamples(ProjectId.parse(request.projectId()),
          request.experimentId())) {
        throw new RequestFailedException(
            "Cannot update experimental group, samples are registered for experiment "
                + request.experimentId());
      }
      experimentInformationService.updateExperimentalGroup(request.projectId(),
          request.experimentId(), convertFromAPI(request.group()));
      return new ExperimentalGroupUpdateResponse(request.experimentId(), request.group(),
          request.requestId());
    });
    String errorMessage = "Error updating experimental group";
    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .retryWhen(defaultRetryStrategy())
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e -> mapToAPIException(e, errorMessage));
  }

  private ExperimentInformationService.ExperimentalGroup convertFromAPI(ExperimentalGroup group) {
    return new ExperimentInformationService.ExperimentalGroup(group.id(), group.groupId(), group.name(),
        group.levels().stream().map(AsyncProjectServiceImpl::convertFromApi).toList(),
        group.sampleSize());
  }

  @Override
  public Mono<ExperimentalGroupDeletionResponse> delete(ExperimentalGroupDeletionRequest request) {
    var call = Mono.fromCallable(() -> {
      if (sampleInfoService.hasSamples(ProjectId.parse(request.projectId()), request.experimentId())) {
        throw new RequestFailedException(
            "Cannot delete experimental group, samples are registered for experiment "
                + request.experimentId());
      }
      experimentInformationService.deleteExperimentalGroupByGroupNumber(request.projectId(), request.experimentId(),
          request.experimentalGroupNumber());

      return new ExperimentalGroupDeletionResponse(request.experimentId(), request.experimentalGroupNumber(),
          request.requestId());
    });
    String errorMessage = "Error deleting experimental group";
    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .retryWhen(defaultRetryStrategy())
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e -> mapToAPIException(e, errorMessage));
  }

  @Override
  public Flux<ExperimentalGroup> getExperimentalGroups(String projectId, String experimentId) {
    var call = Flux.fromStream(() ->
      experimentInformationService.fetchGroups(projectId, ExperimentId.parse(experimentId))
          .stream()
          .map(AsyncProjectServiceImpl::convertToApi));
    String errorMessage = "Error getting experimental group";
    return applySecurityContextMany(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .retryWhen(defaultRetryStrategy())
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e -> mapToAPIException(e, errorMessage));
  }

  private static ExperimentalGroup convertToApi(ExperimentInformationService.ExperimentalGroup group) {
    return new ExperimentalGroup(group.id(), group.groupNumber(), group.name(),
        group.replicateCount(),
        group.levels().stream().map(AsyncProjectServiceImpl::convertToApi).toList());
  }

  private static VariableLevel convertToApi(ExperimentInformationService.VariableLevel level) {
    return new VariableLevel(level.variableName(), level.variableName(), level.unit());
  }

  @Override
  public Mono<ProjectUpdateResponse> update(@NonNull ProjectUpdateRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    var projectId = ProjectId.parse(request.projectId());
    var requestId = request.requestId();
    Mono<ProjectUpdateResponse> response = switch (request.requestBody()) {
      case FundingInformation fundingInformation ->
          update(projectId, requestId, fundingInformation);
      case ProjectManager manager -> update(projectId, requestId, manager);
      case ProjectResponsible responsible -> update(projectId, requestId, responsible);
      case PrincipalInvestigator investigator -> update(projectId, requestId, investigator);
      case ProjectDesign projectDesign -> update(projectId, requestId, projectDesign);
    };
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(response)
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy());
  }

  private Mono<ProjectUpdateResponse> update(ProjectId projectId, String requestId,
      PrincipalInvestigator investigator) {
    return Mono.fromCallable(() -> {
      projectService.investigateProject(projectId, investigator.contact());
      return new ProjectUpdateResponse(projectId.value(), investigator, requestId);
    });
  }

  private Mono<ProjectUpdateResponse> update(ProjectId projectId, String requestId,
      ProjectResponsible responsible) {
    return Mono.fromCallable(() -> {
      projectService.setResponsibility(projectId, responsible.contact());
      return new ProjectUpdateResponse(projectId.value(), responsible, requestId);
    });
  }

  private Mono<ProjectUpdateResponse> update(ProjectId projectId, String requestId,
      ProjectManager manager) {
    return Mono.fromCallable(() -> {
      projectService.manageProject(projectId, manager.contact());
      return new ProjectUpdateResponse(projectId.value(), manager, requestId);
    });
  }

  private Mono<ProjectUpdateResponse> update(ProjectId projectId, String requestId,
      FundingInformation fundingInformation) {
    return Mono.fromCallable(() -> {
      projectService.setFunding(projectId, fundingInformation.grant(),
          fundingInformation.grantId());
      return new ProjectUpdateResponse(projectId.value(), fundingInformation, requestId);
    });
  }

  @Override
  public Mono<ProjectInformation> getProject(String projectId) {
    // TODO implement
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    throw new RuntimeException("not implemented");
  }

  @Override
  public Flux<ByteBuffer> roCrateSummary(String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(() -> getByteBufferFlux(projectId)))
        .contextWrite(reactiveSecurity(securityContext));
  }

  @Override
  public Flux<ExperimentDescription> getExperiments(String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.fromIterable(
            () -> experimentInformationService.findAllForProject(ProjectId.parse(projectId)).iterator())
        .map(e -> e.experimentId())
        .flatMap(id -> {
          var analytes = experimentInformationService.getAnalytesOfExperiment(id);
          var specimen = experimentInformationService.getSpecimensOfExperiment(id);
          var species = experimentInformationService.getSpeciesOfExperiment(id);
          var experimentName = experimentInformationService.find(projectId, id)
              .map(Experiment::getName).orElse("not available");
          return Mono.just(new ExperimentDescription(experimentName, convertToApi(species),
              convertToApi(specimen), convertToApi(analytes)));
        })).subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(securityContext));
  }

  // Requires the SecurityContext to work
  private Flux<ByteBuffer> getByteBufferFlux(String projectId) {
    var search = projectService.find(projectId);
    if (search.isEmpty()) {
      return Flux.empty();
    }
    var project = search.get();
    var digitalObject = digitalObjectFactory.summary(convertToResearchProject(project));
    return Flux.create(
        (FluxSink<ByteBuffer> emitter) -> emitByteBufferFromObject(emitter, digitalObject),
        OverflowStrategy.BUFFER).subscribeOn(VirtualThreadScheduler.getScheduler());
  }

  /**
   * Emits 0..N {@link ByteBuffer} for the content of a {@link DigitalObject}.
   * <p>
   * All bytes are read from the {@link DigitalObject#content()} input stream and converted to byte
   * buffers. The used byte array cache size is 1024 bytes and fixed.
   * <p>
   * After the input stream has been consumed {@link FluxSink#complete()} is called.
   * <p>
   * <p>
   * In case of {@link IOException}: is forwarded via {@link FluxSink#error(Throwable)}.
   *
   * @param emitter the emitter for the byte buffers
   * @param object  the digital object
   */
  private void emitByteBufferFromObject(FluxSink<ByteBuffer> emitter, DigitalObject object) {
    byte[] buffer = new byte[1024];

    try (InputStream content = object.content()) {
      int bytesRead;
      while ((bytesRead = content.read(buffer)) != -1) {
        emitter.next(ByteBuffer.wrap(buffer.clone(), 0, bytesRead));
      }
      emitter.complete();
    } catch (IOException e) {
      emitter.error(e);
    }
  }

  private ContactPoint toContactPoint(Contact contact, String contactType) {
    return ContactPoint.from(contact.fullName(), contact.emailAddress(), contactType);
  }

  private ResearchProject convertToResearchProject(Project project) {
    var contactPoints = new ArrayList<ContactPoint>();
    contactPoints.add(toContactPoint(project.getPrincipalInvestigator(), "Principal Investigator"));
    contactPoints.add(toContactPoint(project.getProjectManager(), "Project Manager"));
    if (project.getResponsiblePerson().isPresent()) {
      contactPoints.add(
          toContactPoint(project.getResponsiblePerson().orElseThrow(), "Responsible Person"));
    }
    return ResearchProject.from(project.getProjectIntent().projectTitle().title(),
        project.getProjectCode().value(), project.getProjectIntent().objective().objective(),
        contactPoints);
  }

  @Override
  public Flux<SamplePreview> getSamplePreviews(String projectId, String experimentId, int offset,
      int limit, List<SortOrder> sortOrders, String filter) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(() ->
        fetchSamplePreviews(projectId, experimentId, offset, limit, sortOrders, filter)))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy());
  }

  private Flux<SamplePreview> fetchSamplePreviews(String projectId, String experimentId, int offset,
      int limit, List<SortOrder> sortOrders, String filter) {
    try {
      return Flux.fromIterable(
          sampleInfoService.queryPreview(ProjectId.parse(projectId),
              ExperimentId.parse(experimentId), offset, limit,
              sortOrders, filter));
    } catch (Exception e) {
      log.error("Error getting sample previews", e);
      return Flux.error(new RequestFailedException("Error getting sample previews"));
    }
  }

  @Override
  public Flux<Sample> getSamples(String projectId, String experimentId)
      throws RequestFailedException {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(() -> fetchSamples(projectId, experimentId)))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext))
        .retryWhen(defaultRetryStrategy());
  }

  // disclaimer: no security context, no scheduler applied
  private Flux<Sample> fetchSamples(String projectId, String experimentId) {
    try {
      return Flux.fromIterable(
          sampleInfoService.retrieveSamplesForExperiment(ProjectId.parse(projectId),
              experimentId));
    } catch (org.springframework.security.access.AccessDeniedException e) {
      log.error("Error getting samples. Access Denied.", e);
      return Flux.error(new AccessDeniedException(ACCESS_DENIED));
    } catch (Exception e) {
      log.error("Unexpected exception getting samples", e);
      return Flux.error(
          new RequestFailedException("Error getting samples for experiment " + experimentId));
    }
  }

  @Override
  public Flux<Sample> getSamplesForBatch(String projectId, String batchId)
      throws RequestFailedException {
    throw new RuntimeException("not implemented");
  }

  @Override
  public Mono<Sample> findSample(String projectId, String sampleId) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    var call = Mono.fromCallable(
            () -> sampleInfoService.findSample(ProjectId.parse(projectId), SampleId.parse(sampleId)))
        .mapNotNull(it -> it.orElse(null));

    String errorMessage = "Error getting sample " + sampleId;
    var resultingMono = applySecurityContext(call)
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e -> mapToAPIException(e, errorMessage))
        .subscribeOn(scheduler);
    if (nonNull(securityContext.getAuthentication())) {
      // we do not want to overwrite the security context when this method is called from a thread with empty security context.
      resultingMono = resultingMono.contextWrite(reactiveSecurity(securityContext));
    }
    return resultingMono;
  }

  @Override
  public Flux<OntologyTerm> getTerms(String value, int offset, int limit) {
    return Flux.defer(() -> Flux.fromIterable(terminologyService.search(value, offset, limit)))
        .map(AsyncProjectServiceImpl::convertToApi)
        .doOnError(e -> log.error("Error searching for term " + value, e))
        .onErrorMap(e -> new RequestFailedException("Error searching for term " + value))
        .subscribeOn(scheduler);
  }

  @Override
  public Mono<OntologyTerm> getTermWithCurie(Curie value) {
    return Mono.defer(() -> Mono.justOrEmpty(terminologyService.findByCurie(value.toString())))
        .map(AsyncProjectServiceImpl::convertToApi)
        .doOnError(e -> log.error("Error searching for term " + value, e))
        .onErrorMap(e -> new RequestFailedException("Error searching for term " + value))
        .subscribeOn(scheduler);
  }

  @Override
  public Flux<OntologyTerm> getTaxa(String value, int offset, int limit, List<SortOrder> sorting) {
    String errorMessage = "Error searching for taxa " + value;
    return Flux.defer(
            () -> Flux.fromIterable(taxaService.queryOntologyTerm(value, offset, limit, sorting)))
        .map(AsyncProjectServiceImpl::convertToApi)
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e -> new RequestFailedException(errorMessage))
        .subscribeOn(scheduler);
  }

  @Override
  public Mono<OntologyTerm> getTaxonWithCurie(Curie value) {
    return Mono.defer(() -> Mono.justOrEmpty(taxaService.findByCURI(value.toString())))
        .map(AsyncProjectServiceImpl::convertToApi)
        .doOnError(e -> log.error("Error searching for taxa " + value, e))
        .onErrorMap(e -> new RequestFailedException("Error searching for taxa " + value))
        .subscribeOn(scheduler);
  }

  @Override
  public Mono<DigitalObject> sampleRegistrationTemplate(String projectId, String experimentId,
      MimeType mimeType) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(Mono.fromCallable(
        () -> templateService.sampleRegistrationTemplate(projectId, experimentId, mimeType)))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext));

  }

  @Override
  public Mono<DigitalObject> sampleUpdateTemplate(String projectId, String experimentId,
      String batchId,
      MimeType mimeType) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(Mono.fromCallable(
        () -> templateService.sampleUpdateTemplate(projectId, experimentId, batchId, mimeType)))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext));
  }

  @Override
  public Mono<DigitalObject> sampleInformationTemplate(String projectId, String experimentId,
      MimeType mimeType) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(Mono.fromCallable(
        () -> templateService.sampleInformationTemplate(projectId, experimentId, mimeType)))
        .subscribeOn(scheduler)
        .contextWrite(reactiveSecurity(securityContext));
  }

  @Override
  public Flux<ValidationResponse> validate(Flux<ValidationRequest> requests)
      throws RequestFailedException {
    return requests.flatMap(this::validateRequest);
  }

  private Mono<ValidationResponse> validateRequest(ValidationRequest request) {
    return switch (request.requestBody()) {
      // Sample Registration
      case SampleRegistrationInformation req ->
          validateSampleMetadata(req, request.requestId(), request.projectId());
      // Sample Update
      case SampleUpdateInformation req ->
          validateSampleMetadataUpdate(req, request.requestId(), request.projectId());
      // Measurement Registration - NGS
      case MeasurementRegistrationInformationNGS req ->
          validateMeasurementMetadataNGS(req, request.requestId(), request.projectId());
      // Measurement Update - NGS
      case MeasurementUpdateInformationNGS req ->
          validateMeasurementMetadataNGSUpdate(req, request.requestId(), request.projectId());
      // Measurement Registration - Proteomics
      case MeasurementRegistrationInformationPxP req ->
          validateMeasurementMetadataPxP(req, request.requestId(), request.projectId());
      // Measurement Update - Proteomics
      case MeasurementUpdateInformationPxP req ->
          validateMeasurementMetadataPxPUpdate(req, request.requestId(), request.projectId());
    };
  }

  /**
   * Ensures that the security context is applied in the correct order and written when it is
   * required.
   * <p>
   * Also ensures that the {@link Callable} is executed on the {@link VirtualThreadScheduler} with
   * {@link Mono#subscribeOn(Scheduler)}.
   *
   * @param securityApplicant
   * @param serviceCallable
   * @param converter
   * @return a {@link Mono} containing the {@link ValidationResponse}
   */
  private Mono<ValidationResponse> validateMetadata(
      UnaryOperator<Mono<ValidationResponse>> securityApplicant,
      Callable<ValidationResult> serviceCallable,
      Function<ValidationResult, ValidationResponse> converter) {
    return securityApplicant.apply(Mono.fromCallable(serviceCallable).map(converter))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataPxP(
      MeasurementRegistrationInformationPxP registration, String requestId,
      String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(Mono.fromCallable(
            () -> measurementValidationService.validatePxp(registration, ProjectId.parse(projectId)))
        .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .contextWrite(reactiveSecurity(securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataPxPUpdate(
      MeasurementUpdateInformationPxP update, String requestId,
      String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(Mono.fromCallable(
            () -> measurementValidationService.validatePxp(update, ProjectId.parse(projectId)))
        .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .contextWrite(reactiveSecurity(securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataNGS(
      MeasurementRegistrationInformationNGS registration, String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(Mono.fromCallable(
            () -> measurementValidationService.validateNGS(registration, ProjectId.parse(projectId)))
        .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .contextWrite(reactiveSecurity(securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataNGSUpdate(
      MeasurementUpdateInformationNGS update, String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return applySecurityContext(Mono.fromCallable(
            () -> measurementValidationService.validateNGS(update, ProjectId.parse(projectId)))
        .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .contextWrite(reactiveSecurity(securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateSampleMetadataUpdate(SampleUpdateInformation update,
      String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return validateMetadata(ReactiveSecurityContextUtils::applySecurityContext,
        () -> sampleValidationService.validateExistingSample(update, ProjectId.parse(projectId))
            .validationResult(),
        result -> new ValidationResponse(requestId, result))
        .contextWrite(reactiveSecurity(securityContext));
  }

  private Mono<ValidationResponse> validateSampleMetadata(
      SampleRegistrationInformation registration, String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return validateMetadata(ReactiveSecurityContextUtils::applySecurityContext,
        () -> sampleValidationService.validateNewSample(registration, ProjectId.parse(projectId))
            .validationResult(),
        result -> new ValidationResponse(requestId, result))
        .contextWrite(reactiveSecurity(securityContext));
  }

  @Override
  public Mono<ExperimentUpdateResponse> update(
      ExperimentUpdateRequest request) {
    Mono<ExperimentUpdateResponse> response = switch (request.body()) {
      case ExperimentDescription experimentDescription ->
          updateExperimentDescription(request.projectId(), request.experimentId(),
              experimentDescription);
      case ConfoundingVariableAdditions confoundingVariableAdditions -> unknownRequest();
      case ConfoundingVariableDeletions confoundingVariableDeletions -> unknownRequest();
      case ConfoundingVariableUpdates confoundingVariableUpdates -> unknownRequest();
    };

    SecurityContext securityContext = SecurityContextHolder.getContext();
    return response
        .retryWhen(defaultRetryStrategy())
        .contextWrite(reactiveSecurity(securityContext));
  }

  @Override
  public Mono<ProjectDeletionResponse> delete(ProjectDeletionRequest request) {
// TODO implement
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public Mono<FundingInformationCreationResponse> create(
      FundingInformationCreationRequest request) {

    var call = Mono.fromCallable(() -> {
      projectService.setFunding(ProjectId.parse(request.projectId()),
          request.information().grant(), request.information().grantId());
      return new FundingInformationCreationResponse(request.requestId(), request.information(),
          request.projectId());
    });

    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error("Could not create funding information", e))
        .onErrorMap(ProjectNotFoundException.class,
            e -> new RequestFailedException("Project was not found"))
        .retryWhen(defaultRetryStrategy());
  }


  @Override
  public Mono<FundingInformationDeletionResponse> delete(
      FundingInformationDeletionRequest request) {
    var call = Mono.fromCallable(() -> {
      projectService.removeFunding(ProjectId.parse(request.projectId()));
      return new FundingInformationDeletionResponse(request.requestId(), request.projectId());
    });

    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error("Could not delete funding information", e))
        .onErrorMap(ProjectNotFoundException.class,
            e -> new RequestFailedException("Project was not found"))
        .retryWhen(defaultRetryStrategy());
  }

  @Override
  public Mono<ProjectResponsibleCreationResponse> create(
      ProjectResponsibleCreationRequest request) {

    var call = Mono.fromCallable(() -> {
      projectService.setResponsibility(ProjectId.parse(request.projectId()),
          request.projectResponsible());
      return new ProjectResponsibleCreationResponse(request.requestId(),
          request.projectResponsible(), request.projectId());
    });

    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error("Could not set responsible person", e))
        .onErrorMap(ProjectNotFoundException.class,
            e -> new RequestFailedException("Project was not found"))
        .retryWhen(defaultRetryStrategy());
  }

  @Override
  public Mono<ProjectResponsibleDeletionResponse> delete(
      ProjectResponsibleDeletionRequest request) {

    var call = Mono.fromCallable(() -> {
      projectService.removeResponsibility(ProjectId.parse(request.projectId()));
      return new ProjectResponsibleDeletionResponse(request.requestId(), request.projectId());
    });

    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error("Could not delete responsible person", e))
        .onErrorMap(ProjectNotFoundException.class,
            e -> new RequestFailedException("Project was not found"))
        .retryWhen(defaultRetryStrategy());
  }

  @Override
  public Mono<ExperimentDeletionResponse> delete(ExperimentDeletionRequest request) {
    // TODO implement
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public Flux<ExperimentalVariable> getExperimentalVariables(String projectId,
      String experimentId) {
    var call = Flux.fromStream(() -> experimentInformationService.getVariablesOfExperiment(projectId,
            ExperimentId.parse(experimentId))
        .stream()
        .map(this::convertToApi));

    return applySecurityContextMany(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error("Could not load experimental variables", e))
        .onErrorMap(org.springframework.security.access.AccessDeniedException.class, e -> new AccessDeniedException(ACCESS_DENIED))
        .onErrorMap(ProjectNotFoundException.class,
            e -> new RequestFailedException("Project was not found"))
        .retryWhen(defaultRetryStrategy());
  }

  @Deprecated
  private ExperimentalVariable convertToApi(
      life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable experimentalVariable) {
    return new ExperimentalVariable(experimentalVariable.name().value(),
        experimentalVariable.levels()
            .stream().map(level -> level.variableName().value()).toList(),
        experimentalVariable.levels().getFirst().experimentalValue().unit().orElse(null));
  }

  @Override
  public Mono<ExperimentalVariablesCreationResponse> create(
      ExperimentalVariablesCreationRequest request) {
    var call = Mono.fromCallable(() -> {
      experimentInformationService.addVariableToExperiment(request.projectId(),
          request.experimentId(), request.experimentalVariables());
      return new ExperimentalVariablesCreationResponse(request.projectId(), request.experimentalVariables(),
        request.experimentId());
    });
    String errorMessage = "Could not create experimental variables";
    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e1 -> mapToAPIException(e1, errorMessage))
        .retryWhen(defaultRetryStrategy());
  }

  @Override
  public Mono<ExperimentalVariableUpdateResponse> update(
      ExperimentalVariableUpdateRequest request) {
//    var call = Mono.fromCallable(() -> {
//      experimentInformationService.updateExperimentalVariable(request.projectId(),
//          request.experimentId(),
//          convertFromApi(request.experimentalVariable()));
//
//      return new ExperimentalVariableUpdateResponse(request.projectId(),
//          request.experimentalVariable(), request.requestId());
//    });
//    return applySecurityContext(call)
//        .subscribeOn(VirtualThreadScheduler.getScheduler())
//        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
//        .doOnError(e -> log.error("Could not update experimental group", e))
//        .retryWhen(defaultRetryStrategy())
//        .onErrorMap(e1 -> mapToAPIException(e1,
//            "Error updating experimental variable " + request.experimentalVariable().name()));
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Mono<ExperimentalVariableRenameResponse> update(
      ExperimentalVariableRenameRequest request) {
    var call = Mono.fromCallable(() -> {
      ExperimentId experimentId = ExperimentId.parse(request.experimentId());
      experimentInformationService.renameExperimentalVariable(request.projectId(),
          experimentId, request.currentVariableName(), request.futureVariableName());

      return new ExperimentalVariableRenameResponse(request.projectId(),
          experimentId.value(), request.currentVariableName(), request.futureVariableName(),
          request.requestId());
    });
    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error("Could not update experimental group", e))
        .retryWhen(defaultRetryStrategy())
        .onErrorMap(e -> mapToAPIException(e,
            "Error renaming experimental variable " + request.currentVariableName() + " to "
                + request.futureVariableName()));
  }

  private ExperimentalVariable convertToApi(
      ExperimentInformationService.ExperimentalVariableInformation experimentalVariable) {
    return new ExperimentalVariable(experimentalVariable.name(), experimentalVariable.levels(),
        experimentalVariable.unit());
  }

  private ExperimentInformationService.ExperimentalVariableInformation convertFromApi(
      String experimentId, ExperimentalVariable experimentalVariable) {
    return new ExperimentInformationService.ExperimentalVariableInformation(experimentId,
        experimentalVariable.name(),
        experimentalVariable.unit(),
        experimentalVariable.levels());
  }

  @Override
  public Mono<ExperimentalVariablesDeletionResponse> delete(
      ExperimentalVariablesDeletionRequest request) {
    var call = Mono.fromCallable(() -> {

      experimentInformationService.deleteAllExperimentalVariables(
          ExperimentId.parse(request.experimentId()),
          ProjectId.parse(request.projectId()));

      return new ExperimentalVariablesDeletionResponse(request.projectId(), request.experimentId(),
          request.requestId());
    });
    String errorMessage = "Could not delete experimental variables";
    return applySecurityContext(call)
        .subscribeOn(VirtualThreadScheduler.getScheduler())
        .contextWrite(reactiveSecurity(SecurityContextHolder.getContext()))
        .doOnError(e -> log.error(errorMessage, e))
        .onErrorMap(e -> mapToAPIException(e, errorMessage))
        .retryWhen(defaultRetryStrategy());
  }

  private Mono<ProjectDeletionResponse> delete(String projectId, String requestId,
      FundingDeletion target) {
    return Mono.defer(() -> {
      try {
        projectService.removeFunding(ProjectId.parse(projectId));
        return Mono.just(new ProjectDeletionResponse(projectId, requestId));
      } catch (org.springframework.security.access.AccessDeniedException e) {
        log.error("Access was denied during deletion of funding information", e);
        return Mono.error(new AccessDeniedException(ACCESS_DENIED));
      } catch (Exception e) {
        log.error("Unexpected exception deleting funding information", e);
        return Mono.error(
            new RequestFailedException("Unexpected exception deleting funding information"));
      }
    });
  }

  private Mono<ProjectDeletionResponse> delete(String projectId, String requestId,
      ProjectResponsibleDeletion request) {
    return Mono.defer(() -> {
      try {
        projectService.removeResponsibility(ProjectId.parse(projectId));
        return Mono.just(new ProjectDeletionResponse(projectId, requestId));
      } catch (org.springframework.security.access.AccessDeniedException e) {
        log.error("Access was denied during deletion of a project responsible", e);
        return Mono.error(new AccessDeniedException(ACCESS_DENIED));
      } catch (Exception e) {
        log.error("Unexpected exception during deletion of a project responsible in request: "
            + requestId, e);
        return Mono.error(new RequestFailedException("Unexpected exception during deletion"));
      }
    });
  }

  private <T> Mono<T> unknownRequest() {
    return Mono.error(() -> new UnknownRequestException("Invalid request body"));
  }

  private Mono<ExperimentUpdateResponse> updateExperimentDescription(String projectId,
      String experimentId,
      ExperimentDescription experimentDescription) {
    // TODO implement
    throw new RuntimeException("Not yet implemented");
  }

  private Mono<ProjectUpdateResponse> update(ProjectId projectId, String requestId,
      ProjectDesign design) {
    return
        Mono.<ProjectUpdateResponse>create(sink -> {
              try {
                projectService.updateTitle(projectId, design.title());
                projectService.updateObjective(projectId, design.objective());
                sink.success(new ProjectUpdateResponse(projectId.value(), design, requestId));
              } catch (IllegalArgumentException e) {
                sink.error(new RequestFailedException("Invalid project id: " + projectId));
              } catch (org.springframework.security.access.AccessDeniedException e) {
                sink.error(new AccessDeniedException(ACCESS_DENIED));
              } catch (RuntimeException e) {
                sink.error(new RequestFailedException("Update project design failed", e));
              }
            }
        );
  }
}
