package life.qbic.authorization;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * TODO
 *
 * @since <version tag>
 */
@Service
public class QbicPermissionEvaluator implements PermissionEvaluator {

  Authentication authentication;
  SimpleGrantedAuthority permission;
  QbicProjectPermissionEvaluator qbicProjectPermissionEvaluator;

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    Objects.requireNonNull(authentication, "Authentication must not be null");
    String requiredAuthority = permission.toString();
    List<String> loadedAuthorities = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).toList();
    if (loadedAuthorities.contains("ROLE_ADMIN")) {
      return true;
    }
    if (loadedAuthorities.contains(requiredAuthority)) {
      return true;
    }
    if (targetDomainObject instanceof Project) {
      QbicUserDetails principal = (QbicUserDetails) authentication.getPrincipal();
      return qbicProjectPermissionEvaluator.hasPermission(principal.getUserId(),
          ((Project) targetDomainObject).getId(), new SimpleGrantedAuthority(requiredAuthority));
    }
    return false;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId,
      String targetType, Object permission) {
    return false;
  }

  public boolean hasSystemRole() {
    return isAdmin();
  }

  //Todo Is this necessary, the global permissions are loaded during login and can't be changed for now
  public boolean isInGlobalPermissions() {
    return authentication.getAuthorities().contains(permission);
  }

  public boolean isInObjectPermissions(Object targetDomainObject) {
    return isInProjectPermissions(targetDomainObject);
  }

  private boolean isAdmin() {
    return authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_Admin"));
  }

  private boolean isInProjectPermissions(Object targetDomainObject) {
    //toDo is this casting necessary can we outsource this to spring itself?
    UserId userId = UserId.from(authentication.getName());
    if (targetDomainObject.getClass().equals(Project.class)) {
      Project project = (Project) targetDomainObject;
      return qbicProjectPermissionEvaluator.hasPermission(userId, project.getId(),
          permission);
    } else if (targetDomainObject.getClass().equals(ProjectPreview.class)) {
      ProjectPreview projectPreview = (ProjectPreview) targetDomainObject;
      return qbicProjectPermissionEvaluator.hasPermission(userId,
          projectPreview.projectId(), permission);
    } else {
      return false;
    }
  }
}
