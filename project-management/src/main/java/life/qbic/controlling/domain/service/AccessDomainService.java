package life.qbic.controlling.domain.service;

import java.util.Objects;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.controlling.domain.model.project.ProjectId;
import life.qbic.controlling.domain.repository.ProjectRepository;
import life.qbic.controlling.domain.service.event.ProjectAccessGranted;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Access Domain Service</b>
 * <p>
 * Service that will emit domain events for project access related tasks, that are not part of
 * domain aggregates.
 *
 * @since 1.0.0
 */
@Service
public class AccessDomainService {

  private final ProjectRepository projectRepository;

  @Autowired
  public AccessDomainService(ProjectRepository projectRepository) {
    this.projectRepository = Objects.requireNonNull(projectRepository);
  }

  /**
   * Inform the domain service, that a user has been granted with access for a certain project.
   *
   * @param projectId the project id of the affected project
   * @param userId    the user that has been granted with access for the project
   * @since 1.0.0
   */
  public void grantProjectAccessFor(String projectId, String userId) {
    var projectTitle = projectRepository.find(ProjectId.parse(projectId)).get().getProjectIntent()
        .projectTitle().title();
    var projectAccessGranted = ProjectAccessGranted.create(userId, projectId, projectTitle);
    DomainEventDispatcher.instance().dispatch(projectAccessGranted);
  }

}
