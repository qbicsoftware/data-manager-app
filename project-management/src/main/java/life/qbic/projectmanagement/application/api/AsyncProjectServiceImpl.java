package life.qbic.projectmanagement.application.api;

import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.applySecurityContext;
import static life.qbic.projectmanagement.application.authorization.ReactiveSecurityContextUtils.writeSecurityContext;

import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class AsyncProjectServiceImpl implements AsyncProjectService {

  private final ProjectInformationService projectService;
  private final Scheduler scheduler;
  private static final Logger log = LoggerFactory.logger(AsyncProjectServiceImpl.class);

  public AsyncProjectServiceImpl(@Autowired ProjectInformationService projectService,
      @Autowired Scheduler scheduler) {
    this.projectService = Objects.requireNonNull(projectService);
    this.scheduler = Objects.requireNonNull(scheduler);
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

  private static Retry defaultRetryStrategy() {
    return Retry.maxInARow(5)
        .doBeforeRetry(retrySignal -> log.warn("Operation failed (" + retrySignal + ")"));
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


  private <T> Mono<T> unknownRequest() {
    return Mono.error(() -> new UnknownRequestException("Invalid request body"));
  }

  private Mono<ProjectUpdateResponse> updateProjectDesign(String projectId, ProjectDesign design, String requestId) {
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
            sink.error(new AccessDeniedException("Access denied"));
          } catch (RuntimeException e) {
            sink.error(new RequestFailedException("Update project design failed", e));
          }
        })
    ).subscribeOn(
        scheduler); //we must not expose the blocking behaviour outside of this method, thus we use a non-blocking scheduler
  }

}
