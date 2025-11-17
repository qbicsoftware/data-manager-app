package life.qbic.datamanager.security;

import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Utility class to check for user permissions. Note: please use {@link org.springframework.security.access.prepost.PreAuthorize},{@link org.springframework.security.access.prepost.PostAuthorize},
 * {@link org.springframework.security.access.prepost.PreFilter}, {@link org.springframework.security.access.prepost.PostFilter} on services and repositories.
 */
public interface UserPermissions {

  boolean readProject(ProjectId projectId);

  boolean editProject(ProjectId projectId);

  boolean changeProjectAccess(ProjectId projectId);

}
