package life.qbic.datamanager.security;

import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public class UserPermissionsImpl implements UserPermissions {

  final AclPermissionEvaluator aclPermissionEvaluator;

  public UserPermissionsImpl(@Autowired
  AclPermissionEvaluator aclPermissionEvaluator) {
    this.aclPermissionEvaluator = aclPermissionEvaluator;
  }

  @Override
  public boolean readProject(ProjectId projectId) {
    return aclPermissionEvaluator.hasPermission(
        SecurityContextHolder.getContext().getAuthentication(), projectId,
        "life.qbic.projectmanagement.domain.project.Project", "READ");
  }

  @Override
  public boolean changeProjectAccess(ProjectId projectId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean hasReadPermission = aclPermissionEvaluator.hasPermission(authentication, projectId,
        "life.qbic.projectmanagement.domain.project.Project", "READ");
    boolean canChangeAclAccess = authentication.getAuthorities().stream()
        .anyMatch(it -> it.getAuthority().equals("acl:change-access"));
    return (hasReadPermission && canChangeAclAccess);
  }
}
