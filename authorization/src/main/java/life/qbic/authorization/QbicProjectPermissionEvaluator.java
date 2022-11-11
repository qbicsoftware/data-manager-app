package life.qbic.authorization;

import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class QbicProjectPermissionEvaluator {

  ProjectPermissionService projectPermissionService;

  public boolean hasPermission(UserId userId,
      ProjectId projectId, SimpleGrantedAuthority permission) {
    return projectPermissionService.loadUserPermissions(userId, projectId).contains(permission);
  }
}
