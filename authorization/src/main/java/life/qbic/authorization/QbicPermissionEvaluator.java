package life.qbic.authorization;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.authorization.security.QbicUserDetails;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * <b>QBiC's implementation of the Spring PermissionEvaluator interface</b>
 * <p>
 * This class shall be used to check if the current user has the permission to have access to the
 * targetDomainObject of interest in the context of user authorization
 */
//@Service
public class QbicPermissionEvaluator implements PermissionEvaluator {

  private final QbicProjectPermissionEvaluator qbicProjectPermissionEvaluator;

  public QbicPermissionEvaluator(
      @Autowired QbicProjectPermissionEvaluator qbicProjectPermissionEvaluator) {
    this.qbicProjectPermissionEvaluator = qbicProjectPermissionEvaluator;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    if (Objects.isNull(targetDomainObject)) {
      return false;
    }
    Object domainObject = targetDomainObject;
    if (targetDomainObject instanceof Optional<?>) {
      if (((Optional<?>) targetDomainObject).isEmpty()) {
        return true; // intentionally empty
      } else {
        domainObject = ((Optional<?>) targetDomainObject).get();
      }
    }
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
    if (domainObject instanceof Project) {
      QbicUserDetails principal = (QbicUserDetails) authentication.getPrincipal();
      if (qbicProjectPermissionEvaluator.hasPermission(principal.getUserId(),
          ((Project) domainObject).getId(), new SimpleGrantedAuthority(requiredAuthority))) {
        return true;
      }
    }
    if (domainObject instanceof ProjectPreview) {
      QbicUserDetails principal = (QbicUserDetails) authentication.getPrincipal();
      return qbicProjectPermissionEvaluator.hasPermission(principal.getUserId(),
          ((ProjectPreview) domainObject).projectId(),
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
