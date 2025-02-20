package life.qbic.datamanager.views.projects.project.info;

import java.util.Objects;
import java.util.function.Supplier;
import life.qbic.datamanager.VirtualThreadScheduler;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

  public AsyncProjectServiceImpl(@Autowired ProjectInformationService projectService) {
    this.projectService = Objects.requireNonNull(projectService);
  }

  @Override
  public Mono<ProjectUpdateResponse> update(@NonNull ProjectUpdateRequest request)
      throws UnknownRequestException, RequestFailedException {
    var projectId = request.projectId();
    switch (request.requestBody()) {
      case ProjectDesign design:
        return writeWithSecurityContext(SecurityContextHolder.getContext(), () -> updateProjectDesign(projectId, design));
      default:
        return Mono.error(new UnknownRequestException("Invalid request body"));
    }
  }

  private Mono<ProjectUpdateResponse> writeWithSecurityContext(SecurityContext sctx, Supplier<Mono<ProjectUpdateResponse>> supplier) {
    var rcontext = ReactiveSecurityContextHolder.withSecurityContext(Mono.just(sctx));
    return ReactiveSecurityContextHolder.getContext().flatMap(securityContext1 -> {
      SecurityContextHolder.setContext(securityContext1);
      return supplier.get();
    }).contextWrite(rcontext).subscribeOn(VirtualThreadScheduler.getScheduler());
  }

  private Mono<ProjectUpdateResponse> updateProjectDesign(String projectId, ProjectDesign design) {
    return Mono.create(sink -> {
      try {
        var id = ProjectId.parse(projectId);
        projectService.updateTitle(id, design.title());
        projectService.updateObjective(id, design.objective());
        sink.success(new ProjectUpdateResponse(projectId, design));
      } catch (IllegalArgumentException e) {
        sink.error(new RequestFailedException("Invalid project id: " + projectId));
      } catch (Exception e) {
        sink.error(new RequestFailedException("Update project design failed", e));
      }
    });
  }
}


