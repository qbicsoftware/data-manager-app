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
   * Creates a reference to a {@link Project} permanently, in order to connect project data.
   *
   * @param projectCode the {@link ProjectCode} of the project reference to store
   * @since 1.0.0
   */
  void add(ProjectCode projectCode);

  /**
   * Searches for projects that contain the provided project code
   *
   * @param projectCode the {@link ProjectCode} to search for in the data repository
   * @return true, if a project with that code already exists in the system, false if not
   * @since 1.0.0
   */
  boolean projectExists(ProjectCode projectCode);

}
