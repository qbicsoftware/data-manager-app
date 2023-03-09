package life.qbic.authorization;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * <b>QBiC's implementation of the Spring PermissionEvaluator interface</b>
 * <p>
 * This class shall be used to check if the current user has the permission to have access to the
 * targetDomainObject of interest in the context of user authorization
 */
@Service
public class QbicPermissionEvaluator implements PermissionEvaluator {

  private final QbicProjectPermissionEvaluator qbicProjectPermissionEvaluator;

  public QbicPermissionEvaluator(
      @Autowired QbicProjectPermissionEvaluator qbicProjectPermissionEvaluator) {
    this.qbicProjectPermissionEvaluator = qbicProjectPermissionEvaluator;
  }

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
      if (qbicProjectPermissionEvaluator.hasPermission(principal.getUserId(),
          ((Project) targetDomainObject).getId(), new SimpleGrantedAuthority(requiredAuthority))) {
        return true;
      }
    }
    if (targetDomainObject instanceof ProjectPreview) {
      QbicUserDetails principal = (QbicUserDetails) authentication.getPrincipal();
      return qbicProjectPermissionEvaluator.hasPermission(principal.getUserId(),
          ((ProjectPreview) targetDomainObject).projectId(),
          new SimpleGrantedAuthority(requiredAuthority));
    }
    return false;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId,
      String targetType, Object permission) {
    return false;
  }
}
