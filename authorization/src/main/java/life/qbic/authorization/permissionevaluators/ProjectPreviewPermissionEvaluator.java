package life.qbic.authorization.permissionevaluators;

import java.io.Serializable;
import life.qbic.projectmanagement.application.ProjectPreview;
import life.qbic.projectmanagement.domain.project.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProjectPreviewPermissionEvaluator implements PermissionEvaluator {

  private final ProjectPermissionEvaluator projectPermissionEvaluator;

  public ProjectPreviewPermissionEvaluator(
      @Autowired ProjectPermissionEvaluator projectPermissionEvaluator) {
    this.projectPermissionEvaluator = projectPermissionEvaluator;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Object targetDomainObject,
      Object permission) {
    if (targetDomainObject instanceof ProjectPreview) {
      return projectPermissionEvaluator.hasPermission(authentication,
          ((ProjectPreview) targetDomainObject).projectId(),
          Project.class.getName(),
          permission);
    }
    return false;
  }

  @Override
  public boolean hasPermission(Authentication authentication, Serializable targetId,
      String targetType, Object permission) {
    if (targetType.equals(ProjectPreview.class.getName())) {
      return projectPermissionEvaluator.hasPermission(authentication, targetId,
          Project.class.getName(),
          permission);
    }
    return false;
  }
}
