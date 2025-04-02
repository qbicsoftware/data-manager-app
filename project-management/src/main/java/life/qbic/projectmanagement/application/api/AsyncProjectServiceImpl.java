package life.qbic.projectmanagement.application.api;

import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContextMany;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.writeSecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.writeSecurityContextMany;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.UnaryOperator;
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
import life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils;
import life.qbic.projectmanagement.application.api.template.TemplateService;
import life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SampleValidationService;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.reactivestreams.Publisher;
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

  public AsyncProjectServiceImpl(
      @Autowired ProjectInformationService projectService,
      @Autowired SampleInformationService sampleInfoService,
      @Autowired Scheduler scheduler,
      @Autowired DigitalObjectFactory digitalObjectFactory,
      @Autowired TemplateService templateService,
      @Autowired SampleValidationService sampleValidationService,
      @Autowired MeasurementValidationService measurementValidationService
  ) {
    this.projectService = Objects.requireNonNull(projectService);
    this.sampleInfoService = Objects.requireNonNull(sampleInfoService);
    this.scheduler = Objects.requireNonNull(scheduler);
    this.digitalObjectFactory = Objects.requireNonNull(digitalObjectFactory);
    this.templateService = Objects.requireNonNull(templateService);
    this.sampleValidationService = Objects.requireNonNull(sampleValidationService);
    this.measurementValidationService = Objects.requireNonNull(measurementValidationService);
  }

  private static Retry defaultRetryStrategy() {
    return Retry.maxInARow(5)
        .doBeforeRetry(retrySignal -> log.warn("Operation failed (" + retrySignal + ")"));
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
    return ReactiveSecurityContextUtils.applySecurityContext(response)
        .subscribeOn(scheduler)
        .transform(original -> writeSecurityContext(original, securityContext))
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
  public Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    //TODO
    throw new RuntimeException("not implemented");
  }

  @Override
  public Flux<ByteBuffer> roCrateSummary(String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(Flux.defer(() -> getByteBufferFlux(projectId))).transform(
        original -> writeSecurityContextMany(original, securityContext));
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
        .transform(original -> writeSecurityContextMany(original, securityContext))
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
        .transform(original -> writeSecurityContextMany(original, securityContext))
        .retryWhen(defaultRetryStrategy());
  }

  // disclaimer: no security context, no scheduler applied
  private Flux<Sample> fetchSamples(String projectId, String experimentId) {
    try {
      return Flux.fromIterable(
          sampleInfoService.retrieveSamplesForExperiment(ProjectId.parse(projectId),
              experimentId));
    } catch (org.springframework.security.access.AccessDeniedException e) {
      log.error("Error getting samples", e);
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
    return Mono.defer(() -> {
      try {
        return Mono.justOrEmpty(
            sampleInfoService.findSample(ProjectId.parse(projectId), SampleId.parse(sampleId)));
      } catch (org.springframework.security.access.AccessDeniedException e) {
        log.error(ACCESS_DENIED, e);
        return Mono.error(new AccessDeniedException(ACCESS_DENIED));
      } catch (Exception e) {
        log.error("Error getting sample for sample " + sampleId, e);
        return Mono.error(
            new RequestFailedException("Error getting sample for sample " + sampleId));
      }
    }).subscribeOn(scheduler);
  }

  @Override
  public Mono<DigitalObject> sampleRegistrationTemplate(String projectId, String experimentId,
      MimeType mimeType) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(Mono.fromCallable(
            () -> templateService.sampleRegistrationTemplate(projectId, experimentId, mimeType)))
        .subscribeOn(scheduler)
        .transform(original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
            securityContext));
  }

  @Override
  public Mono<DigitalObject> sampleUpdateTemplate(String projectId, String experimentId, String batchId,
      MimeType mimeType) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(Mono.fromCallable(
            () -> templateService.sampleUpdateTemplate(projectId, experimentId, batchId, mimeType)))
        .subscribeOn(scheduler)
        .transform(original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
            securityContext));
  }

  @Override
  public Mono<DigitalObject> sampleInformationTemplate(String projectId, String experimentId,
      MimeType mimeType) {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(Mono.fromCallable(
            () -> templateService.sampleInformationTemplate(projectId, experimentId, mimeType)))
        .subscribeOn(scheduler)
        .transform(original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
            securityContext));
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
      default -> Mono.error(new RequestFailedException("Invalid request"));
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
   * @param transformer
   * @return a {@link Mono} containing the
   * {@link life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationResponse}
   */
  private Mono<ValidationResponse> validateMetadata(
      UnaryOperator<Mono<ValidationResponse>> securityApplicant,
      Callable<ValidationResult> serviceCallable,
      Function<ValidationResult, ValidationResponse> converter,
      Function<Mono<ValidationResponse>, Publisher<ValidationResponse>> transformer) {
    return securityApplicant.apply(Mono.fromCallable(serviceCallable).map(converter))
        .transform(transformer).subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataPxP(
      MeasurementRegistrationInformationPxP registration, String requestId,
      String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(Mono.fromCallable(
                () -> measurementValidationService.validatePxp(registration, ProjectId.parse(projectId)))
            .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .transform(
            original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
                securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataPxPUpdate(
      MeasurementUpdateInformationPxP update, String requestId,
      String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(Mono.fromCallable(
                () -> measurementValidationService.validatePxp(update, ProjectId.parse(projectId)))
            .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .transform(
            original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
                securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataNGS(
      MeasurementRegistrationInformationNGS registration, String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(Mono.fromCallable(
                () -> measurementValidationService.validateNGS(registration, ProjectId.parse(projectId)))
            .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .transform(
            original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
                securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateMeasurementMetadataNGSUpdate(
      MeasurementUpdateInformationNGS update, String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(Mono.fromCallable(
                () -> measurementValidationService.validateNGS(update, ProjectId.parse(projectId)))
            .map(validationResult -> new ValidationResponse(requestId, validationResult)))
        .transform(
            original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
                securityContext))
        .subscribeOn(scheduler);
  }

  private Mono<ValidationResponse> validateSampleMetadataUpdate(SampleUpdateInformation update,
      String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return validateMetadata(ReactiveSecurityContextUtils::applySecurityContext,
        () -> sampleValidationService.validateExistingSample(update, ProjectId.parse(projectId))
            .validationResult(),
        result -> new ValidationResponse(requestId, result),
        original -> ReactiveSecurityContextUtils.writeSecurityContext(original, securityContext)
    );
  }

  private Mono<ValidationResponse> validateSampleMetadata(
      SampleRegistrationInformation registration, String requestId, String projectId) {
    var securityContext = SecurityContextHolder.getContext();
    return validateMetadata(ReactiveSecurityContextUtils::applySecurityContext,
        () -> sampleValidationService.validateNewSample(registration, ProjectId.parse(projectId))
            .validationResult(),
        result -> new ValidationResponse(requestId, result),
        original -> ReactiveSecurityContextUtils.writeSecurityContext(original, securityContext));
  }

  @Override
  public Mono<ExperimentUpdateResponse> update(
      ExperimentUpdateRequest request) {
    Mono<ExperimentUpdateResponse> response = switch (request.body()) {

      case ExperimentDescription experimentDescription ->
          updateExperimentDescription(request.projectId(), request.experimentId(),
              experimentDescription);

      case ExperimentalGroups experimentalGroups -> unknownRequest();
      case ConfoundingVariableAdditions confoundingVariableAdditions -> unknownRequest();
      case ConfoundingVariableDeletions confoundingVariableDeletions -> unknownRequest();
      case ConfoundingVariableUpdates confoundingVariableUpdates -> unknownRequest();
      case ExperimentalVariableAdditions experimentalVariableAdditions -> unknownRequest();
      case ExperimentalVariableDeletions experimentalVariableDeletions -> unknownRequest();
    };

    SecurityContext securityContext = SecurityContextHolder.getContext();
    return response
        .transform(original -> writeSecurityContext(original, securityContext))
        .retryWhen(defaultRetryStrategy());
  }

  @Override
  public Mono<ProjectDeletionResponse> delete(ProjectDeletionRequest request) {
    Mono<ProjectDeletionResponse> responseMono = switch (request.body()) {
      case ProjectResponsibleDeletion target ->
          delete(request.projectId(), request.requestId(), target);
      case FundingDeletion target -> delete(request.projectId(), request.requestId(), target);
    };
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(responseMono)
        .transform(original -> writeSecurityContext(original, securityContext))
        .retryWhen(defaultRetryStrategy())
        .subscribeOn(scheduler);
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
        return Mono.error(new RequestFailedException("Unexpected exception deleting funding information"));
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
    //TODO implement
    throw new RuntimeException("Not implemented");
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
