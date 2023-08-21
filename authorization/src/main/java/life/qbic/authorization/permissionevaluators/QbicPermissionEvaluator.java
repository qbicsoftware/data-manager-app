package life.qbic.authorization.permissionevaluators;

import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * <b>QBiC's implementation of the Spring PermissionEvaluator interface</b>
 * <p>
 * This class shall be used to check if the current user has the permission to have access to the
 * targetDomainObject of interest in the context of user authorization
 */
@Service
public class QbicPermissionEvaluator implements PermissionEvaluator {

  private final Map<String, PermissionEvaluator> evaluatorMap;


  public QbicPermissionEvaluator(
      @Autowired ProjectPermissionEvaluator projectPermissionEvaluator,
      @Autowired ProjectPreviewPermissionEvaluator projectPreviewPermissionEvaluator) {
    evaluatorMap = Map.ofEntries(
        entry(Project.class.getName(), projectPermissionEvaluator),
        entry(ProjectPreview.class.getName(), projectPreviewPermissionEvaluator)
    );
  }

  private boolean hasSystemPermission(Authentication authentication,
      String requiredAuthority) {
    requireNonNull(authentication, "Authentication must not be null");
    List<String> loadedAuthorities = authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority).toList();
    if (loadedAuthorities.contains("ROLE_ADMIN")) {
      return true;
    }
    return loadedAuthorities.contains(requiredAuthority);
  }

  private static boolean isEmptyOptional(Object object) {
    if (object instanceof Optional<?>) {
      return ((Optional<?>) object).isEmpty();
    }
    return false;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    requireNonNull(authentication, "authentication must not be null");
    requireNonNull(permission, "permission must not be null");
    //forbid null
    if (Objects.isNull(targetDomainObject)) {
      return false;
    }
    if (isEmptyOptional(targetDomainObject)) {
      return true;
    }
    if (hasSystemPermission(authentication, permission.toString())) {
      return true;
    }
    PermissionEvaluator evaluator = evaluatorMap.get(targetDomainObject.getClass().getName());
    if (evaluator == null) {
      return false;
    }
    return evaluator.hasPermission(authentication, targetDomainObject, permission);
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId,
      String targetType, Object permission) {
    requireNonNull(authentication, "authentication must not be null");
    requireNonNull(targetId, "targetId must not be null");
    requireNonNull(targetType, "targetType must not be null");
    requireNonNull(permission, "permission must not be null");
    if (hasSystemPermission(authentication, permission.toString())) {
      return true;
    }
    PermissionEvaluator evaluator = evaluatorMap.get(targetType);
    if (evaluator == null) {
      return false;
    }
    return evaluator.hasPermission(authentication, targetId, targetType, permission);
  }
}
