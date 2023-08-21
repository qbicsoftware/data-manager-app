package life.qbic.authorization.permissionevaluators;

import java.io.Serializable;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authorization.ProjectPermissionService;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class ProjectPermissionEvaluator implements PermissionEvaluator {

  private final ProjectPermissionService projectPermissionService;

  public ProjectPermissionEvaluator(
      @Autowired ProjectPermissionService projectPermissionService) {
    this.projectPermissionService = projectPermissionService;
  }

  public boolean hasPermission(UserId userId,
      ProjectId projectId, SimpleGrantedAuthority permission) {
    return projectPermissionService.loadUserPermissions(userId, projectId).stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(it -> it.equals(permission.getAuthority()));
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    if (targetDomainObject instanceof Project) {
      return hasPermission(((QbicUserDetails) authentication.getPrincipal()).getUserId(),
          ((Project) targetDomainObject).getId(),
          new SimpleGrantedAuthority(permission.toString()));
    }
    return false;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId,
      String targetType, Object permission) {
    if (targetId instanceof ProjectId) {
      return hasPermission(((QbicUserDetails) authentication.getPrincipal()).getUserId(),
          (ProjectId) targetId,
          new SimpleGrantedAuthority(permission.toString()));
    }
    return false;
  }
}
