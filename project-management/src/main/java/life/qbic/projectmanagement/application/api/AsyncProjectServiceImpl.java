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
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.VirtualThreadScheduler;
import life.qbic.projectmanagement.application.api.fair.ContactPoint;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.api.fair.DigitalObjectFactory;
import life.qbic.projectmanagement.application.api.fair.ResearchProject;
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

  public static final String ACCESS_DENIED = "Access denied";
  private static final Logger log = LoggerFactory.logger(AsyncProjectServiceImpl.class);
  private final ProjectInformationService projectService;
  private final Scheduler scheduler;
  private final SampleInformationService sampleInfoService;
  private final DigitalObjectFactory digitalObjectFactory;
  private final SampleValidationService sampleValidationService;

  public AsyncProjectServiceImpl(@Autowired ProjectInformationService projectService,
      @Autowired SampleInformationService sampleInfoService,
      @Autowired Scheduler scheduler, @Autowired DigitalObjectFactory digitalObjectFactory,
      @Autowired SampleValidationService sampleValidationService) {
    this.projectService = Objects.requireNonNull(projectService);
    this.sampleInfoService = Objects.requireNonNull(sampleInfoService);
    this.scheduler = Objects.requireNonNull(scheduler);
    this.digitalObjectFactory = Objects.requireNonNull(digitalObjectFactory);
    this.sampleValidationService = Objects.requireNonNull(sampleValidationService);
  }

  private static Retry defaultRetryStrategy() {
    return Retry.maxInARow(5)
        .doBeforeRetry(retrySignal -> log.warn("Operation failed (" + retrySignal + ")"));
  }

  @Override
  public Mono<ProjectUpdateResponse> update(@NonNull ProjectUpdateRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    var projectId = request.projectId();
    Mono<ProjectUpdateResponse> response = switch (request.requestBody()) {
      case FundingInformation fundingInformation -> unknownRequest();
      case ProjectContacts projectContacts -> unknownRequest();
      case ProjectDesign projectDesign ->
          updateProjectDesign(projectId, projectDesign, request.requestId());
    };
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return response
        .transform(original -> writeSecurityContext(original, securityContext))
        .retryWhen(defaultRetryStrategy());
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
    return applySecurityContextMany(Flux.defer(() -> getByteBufferFlux(projectId)))
        .transform(original -> writeSecurityContextMany(original, securityContext));
  }

  // Requires the SecurityContext to work
  private Flux<ByteBuffer> getByteBufferFlux(String projectId) {
    var search = projectService.find(projectId);
    if (search.isEmpty()) {
      return Flux.empty();
    }
    var project = search.get();
    var digitalObject = digitalObjectFactory.summary(convertToResearchProject(project));
    return Flux
        .create((FluxSink<ByteBuffer> emitter) -> emitByteBufferFromObject(emitter, digitalObject),
            OverflowStrategy.BUFFER)
        .subscribeOn(VirtualThreadScheduler.getScheduler());
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
    throw new RuntimeException("not implemented");
  }

  @Override
  public Mono<DigitalObject> sampleUpdateTemplate(String projectId, String experimentId,
      MimeType mimeType) {
    throw new RuntimeException("not implemented");
  }

  @Override
  public Flux<ValidationResponse> validate(Flux<ValidationRequest> requests)
      throws RequestFailedException {
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return applySecurityContextMany(requests).transform(original -> writeSecurityContextMany(original, securityContext)).flatMap(this::validateRequest);
  }

  private Mono<ValidationResponse> validateRequest(ValidationRequest request) {
    switch (request.requestBody()) {
      case SampleRegistrationRequest r:
        return validateSampleMetadata(r, request.requestId());
      default:
        return Mono.error(new RequestFailedException("Invalid request"));
    }
  }


  private Mono<ValidationResponse> validateSampleMetadata(SampleRegistrationRequest registration,
      String requestId) {
    var securityContext = SecurityContextHolder.getContext();
    return ReactiveSecurityContextUtils.applySecurityContext(
            Mono.fromCallable(
                    () -> sampleValidationService.validateSample(registration).validationResult()
                )
                .map(validationResult -> new ValidationResponse(requestId, validationResult))
                .subscribeOn(scheduler))
        .transform(original -> ReactiveSecurityContextUtils.writeSecurityContext(original,
            securityContext));
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

  private <T> Mono<T> unknownRequest() {
    return Mono.error(() -> new UnknownRequestException("Invalid request body"));
  }

  private Mono<ExperimentUpdateResponse> updateExperimentDescription(String projectId,
      String experimentId,
      ExperimentDescription experimentDescription) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }


  private Mono<ProjectUpdateResponse> updateProjectDesign(String projectId, ProjectDesign design,
      String requestId) {
    return applySecurityContext(
        Mono.<ProjectUpdateResponse>create(sink -> {
          try {
            var id = ProjectId.parse(projectId);
            projectService.updateTitle(id, design.title());
            projectService.updateObjective(id, design.objective());
            sink.success(new ProjectUpdateResponse(projectId, design, requestId));
          } catch (IllegalArgumentException e) {
            sink.error(new RequestFailedException("Invalid project id: " + projectId));
          } catch (org.springframework.security.access.AccessDeniedException e) {
            sink.error(new AccessDeniedException(ACCESS_DENIED));
          } catch (RuntimeException e) {
            sink.error(new RequestFailedException("Update project design failed", e));
          }
        })
    ).subscribeOn(
        scheduler); //we must not expose the blocking behaviour outside of this method, thus we use a non-blocking scheduler
  }

}
