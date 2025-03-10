package life.qbic.projectmanagement.application.api;

import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContextMany;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.writeSecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.writeSecurityContextMany;

import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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

  public AsyncProjectServiceImpl(@Autowired ProjectInformationService projectService,
      @Autowired SampleInformationService sampleInfoService,
      @Autowired Scheduler scheduler) {
    this.projectService = Objects.requireNonNull(projectService);
    this.sampleInfoService = Objects.requireNonNull(sampleInfoService);
    this.scheduler = Objects.requireNonNull(scheduler);
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
  public Flux<SamplePreview> getSamplePreviews(String projectId, String experimentId, int offset,
      int limit, List<SortOrder> sortOrders, String filter) {
    return Flux.defer(() -> {
      try {
        return Flux.fromIterable(
            sampleInfoService.queryPreview(ExperimentId.parse(experimentId), offset, limit,
                sortOrders, filter));
      } catch (Exception e) {
        log.error("Error getting sample previews", e);
        return Flux.error(new RequestFailedException("Error getting sample previews"));
      }
    }).subscribeOn(scheduler);
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
        return Mono.justOrEmpty(sampleInfoService.findSample(SampleId.parse(sampleId)));
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
  public Mono<ExperimentUpdateResponse> update(
      ExperimentUpdateRequest request) {
    Mono<ExperimentUpdateResponse> response = switch (request.body()) {
      case ExperimentalVariables experimentalVariables ->
          updateExperimentalVariables(request.projectId(), request.experimentId(),
              experimentalVariables);
      case ExperimentDescription experimentDescription ->
          updateExperimentDescription(request.projectId(), request.experimentId(),
              experimentDescription);

      case ConfoundingVariables confoundingVariables ->
          updateConfoundingVariables(request.projectId(), request.experimentId(),
              confoundingVariables);
      case ExperimentalGroups experimentalGroups -> unknownRequest();
    };

    SecurityContext securityContext = SecurityContextHolder.getContext();
    return response
        .transform(original -> writeSecurityContext(original, securityContext))
        .retryWhen(defaultRetryStrategy());
  }

  private <T> Mono<T> unknownRequest() {
    return Mono.error(() -> new UnknownRequestException("Invalid request body"));
  }

  private Mono<ExperimentUpdateResponse> updateConfoundingVariables(String projectId,
      String experimentId,
      ConfoundingVariables confoundingVariables) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  private Mono<ExperimentUpdateResponse> updateExperimentDescription(String projectId,
      String experimentId,
      ExperimentDescription experimentDescription) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  private Mono<ExperimentUpdateResponse> updateExperimentalVariables(String projectId,
      String experimentId, ExperimentalVariables experimentalVariables) {
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
