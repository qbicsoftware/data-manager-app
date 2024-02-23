package life.qbic.datamanager.security;

import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserPermissionsImpl implements UserPermissions {

  final AclPermissionEvaluator aclPermissionEvaluator;

  final private String projectTargetType = "life.qbic.projectmanagement.domain.model.project.Project";

  public UserPermissionsImpl(@Autowired
  AclPermissionEvaluator aclPermissionEvaluator) {
    this.aclPermissionEvaluator = aclPermissionEvaluator;
  }

  @Override
  public boolean readProject(ProjectId projectId) {
    return aclPermissionEvaluator.hasPermission(
        SecurityContextHolder.getContext().getAuthentication(), projectId,
        projectTargetType, "READ");
  }

  @Override
  public boolean changeProjectAccess(ProjectId projectId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean hasReadPermission = aclPermissionEvaluator.hasPermission(authentication, projectId,
        projectTargetType, "READ");
    boolean hasCreatedProject = aclPermissionEvaluator.hasPermission(authentication, projectId,
        projectTargetType, "ADMINISTRATION");
    boolean canChangeAclAccess = authentication.getAuthorities().stream()
        .anyMatch(it -> it.getAuthority().equals("acl:change-access"));
    return (hasReadPermission && (canChangeAclAccess || hasCreatedProject));
  }
}
