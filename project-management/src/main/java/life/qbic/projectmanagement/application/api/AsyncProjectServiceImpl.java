package life.qbic.projectmanagement.application.api;

import java.util.Objects;
import java.util.function.Supplier;
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
    switch (request.requestBody()) {
      case ProjectDesign design:
        return
            withSecurityContext(SecurityContextHolder.getContext(),
                () -> updateProjectDesign(projectId, design, request.requestId())).subscribeOn(
                scheduler);
      default:
        return Mono.error(new UnknownRequestException("Invalid request body"));
    }
  }

  @Override
  public Mono<ProjectCreationResponse> create(ProjectCreationRequest request)
      throws UnknownRequestException, RequestFailedException, AccessDeniedException {
    throw new RuntimeException("not implemented");
  }

  /*
  Configures and writes the provided security context for a supplier of type Mono<ProjectUpdateResponse>. Without
  the context written to the reactive stream, services that have access control methods will fail.
   */
  private Mono<ProjectUpdateResponse> withSecurityContext(SecurityContext sctx,
      Supplier<Mono<ProjectUpdateResponse>> supplier) {
    var rcontext = ReactiveSecurityContextHolder.withSecurityContext(Mono.just(sctx));
    return ReactiveSecurityContextHolder.getContext().flatMap(securityContext1 -> {
      SecurityContextHolder.setContext(securityContext1);
      return supplier.get();
    }).contextWrite(rcontext);
  }

  private Mono<ProjectUpdateResponse> updateProjectDesign(String projectId, ProjectDesign design,
      String requestId) {
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


