package life.qbic.datamanager.security;

import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * Utility class to check for user permissions. Note: please use @PreAuthorize, @PostAuthorize,
 * @PreFilter, @PostFilter on services and repositories.
 */
public interface UserPermissions {

  boolean readProject(ProjectId projectId);

  boolean changeProjectAccess(ProjectId projectId);

}
