package life.qbic.authorization;

import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class QbicProjectPermissionEvaluator {

  private final ProjectPermissionService projectPermissionService;

  public QbicProjectPermissionEvaluator(
      @Autowired ProjectPermissionService projectPermissionService) {
    this.projectPermissionService = projectPermissionService;
  }

  public boolean hasPermission(UserId userId,
                               ProjectId projectId, SimpleGrantedAuthority permission) {
    return projectPermissionService.loadUserPermissions(userId, projectId).stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(it -> it.equals(permission.getAuthority()));
  }
}
