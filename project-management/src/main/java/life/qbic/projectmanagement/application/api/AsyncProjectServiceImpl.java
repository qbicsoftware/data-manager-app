package life.qbic.projectmanagement.application.api;

import java.util.Objects;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

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

  public AsyncProjectServiceImpl(@Autowired ProjectInformationService projectService,
      @Autowired Scheduler scheduler) {
    this.projectService = Objects.requireNonNull(projectService);
    this.scheduler = Objects.requireNonNull(scheduler);
  }

  @Override
  public Mono<ProjectUpdateResponse> update(@NonNull ProjectUpdateRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    var projectId = request.projectId();
    final var securityContext = SecurityContextHolder.getContext();
    return switch (request.requestBody()) {
      case ProjectDesign design ->
          Mono.defer(() -> updateProjectDesign(projectId, design, request.requestId()))
              .transform(original -> withSecurityContext(securityContext, original));
      case FundingInformation fundingInformation -> unknownRequest();
      case ProjectContacts projectContacts -> unknownRequest();
    };
  }

  private <T> Mono<T> unknownRequest() {
    return Mono.error(() -> new UnknownRequestException("Invalid request body"));
  }

  private <T> Mono<T> withSecurityContext(final SecurityContext securityContext, Mono<T> original) {
    return original.contextWrite(
        ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
  }

  @Override
  public Mono<ExperimentUpdateResponse> update(
      ExperimentUpdateRequest request) {
    return Mono.fromSupplier(() -> switch (request.body()) {
      case ExperimentalVariables experimentalVariables ->
          updateExperimentalVariables(request.projectId(), request.experimentId(),
              experimentalVariables);
      case ExperimentDescription experimentDescription ->
          updateExperimentDescription(request.projectId(), request.experimentId(),
              experimentDescription);

      case ConfoundingVariables confoundingVariables ->
          updateConfoundingVariables(request.projectId(), request.experimentId(),
              confoundingVariables);
    }).subscribeOn(Schedulers.boundedElastic());
  }

  private ExperimentUpdateResponse updateConfoundingVariables(String projectId, String experimentId,
      ConfoundingVariables confoundingVariables) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  private ExperimentUpdateResponse updateExperimentDescription(String projectId,
      String experimentId,
      ExperimentDescription experimentDescription) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  private ExperimentUpdateResponse updateExperimentalVariables(String projectId,
      String experimentId, ExperimentalVariables experimentalVariables) {
    //TODO implement
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    throw new RuntimeException("not implemented");
  }

  private Mono<ProjectUpdateResponse> updateProjectDesign(String projectId, ProjectDesign design, String requestId) {
    return Mono.create(sink -> {
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
    });
  }
}
