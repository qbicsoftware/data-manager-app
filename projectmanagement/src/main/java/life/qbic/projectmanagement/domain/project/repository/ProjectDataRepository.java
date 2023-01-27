package life.qbic.projectmanagement.domain.project.repository;

import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;


/**
 * <b>Project Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link Project} data storage.
 *
 * @since 1.0.0
 */
public interface ProjectDataRepository {

  /**
   * Saves a {@link Project} entity permanently.
   *
   * @param project the project to store
   * @since 1.0.0
   */
  void add(Project project);

  /**
   * Searches for projects that contain the provided project code
   *
   * @param projectCode the project code to search for in projects
   * @return projects that contain the project code
   * @since 1.0.0
   */
  boolean projectExists(ProjectCode projectCode);

}
