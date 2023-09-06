package life.qbic.projectmanagement.domain.project.service;

import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.project.service.event.ProjectAccessGranted;
import org.springframework.stereotype.Service;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class AccessDomainService {

  public AccessDomainService() {
  }
  public void grantProjectAccessFor(String projectId, String userId) {
    var projectAccessGranted = ProjectAccessGranted.create(userId, projectId);
    DomainEventDispatcher.instance().dispatch(projectAccessGranted);
  }

}
