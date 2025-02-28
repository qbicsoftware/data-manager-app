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
 * Implementation of the {@link AsyncProjectService} interface.
 * <p>
 * This is the class that should make the actual individual service orchestration and gets all
 * services injected.
 *
 * @since 1.9.0
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
    Mono<ProjectUpdateResponse> response = switch (request.requestBody()) {
      case FundingInformation fundingInformation -> unknownRequest();
      case ProjectContacts projectContacts -> unknownRequest();
      case ProjectDesign projectDesign ->
          updateProjectDesign(projectId, projectDesign, request.requestId());
    };
    SecurityContext securityContext = SecurityContextHolder.getContext();
    return response
        .transform(original -> writeSecurityContext(original, securityContext))
        .retryWhen(Retry.maxInARow(5)
            .doBeforeRetry(retrySignal -> log.warn("Update failed (" + retrySignal + ")")));
  }

  @Override
  public Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    throw new RuntimeException("not implemented");
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
