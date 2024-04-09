package life.qbic.datamanager.security;

import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.AclPermissionEvaluator;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserPermissionsImpl implements UserPermissions {

  final AclPermissionEvaluator aclPermissionEvaluator;

  private static final String PROJECT_TARGET_TYPE = "life.qbic.projectmanagement.domain.model.project.Project";

  public UserPermissionsImpl(@Autowired
  AclPermissionEvaluator aclPermissionEvaluator) {
    this.aclPermissionEvaluator = aclPermissionEvaluator;
  }

  @Override
  public boolean readProject(ProjectId projectId) {
    return aclPermissionEvaluator.hasPermission(
        SecurityContextHolder.getContext().getAuthentication(), projectId, PROJECT_TARGET_TYPE,
        BasePermission.READ);
  }

  @Override
  public boolean editProject(ProjectId projectId) {
    return aclPermissionEvaluator.hasPermission(
        SecurityContextHolder.getContext().getAuthentication(), projectId, PROJECT_TARGET_TYPE,
        BasePermission.WRITE);
  }

  @Override
  public boolean changeProjectAccess(ProjectId projectId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean hasReadPermission = aclPermissionEvaluator.hasPermission(authentication, projectId,
        PROJECT_TARGET_TYPE, BasePermission.READ);
    boolean administratesProject = aclPermissionEvaluator.hasPermission(authentication, projectId,
        PROJECT_TARGET_TYPE, BasePermission.ADMINISTRATION);
    return (hasReadPermission && administratesProject);
  }
}
