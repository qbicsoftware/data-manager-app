package life.qbic.projectmanagement.infrastructure.project;

import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;


/**
 * <b>Project Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link Project} data storage.
 *
 * @since 1.0.0
 */
public interface QbicProjectDataRepo {

  /**
   * Creates a reference to a {@link Project} in the data repository to connect project data.
   *
   * @param project the {@link Project} for which a reference should be created
   * @since 1.0.0
   */
  void add(Project project);

  /**
   * Deletes a project with the provided code from persistence.
   *
   * @param projectCode the {@link ProjectCode} of the project to delete
   * @since 1.0.0
   */
  void delete(ProjectCode projectCode);

  /**
   * Searches for projects that contain the provided project code
   *
   * @param projectCode the {@link ProjectCode} to search for in the data repository
   * @return true, if a project with that code already exists in the system, false if not
   * @since 1.0.0
   */
  boolean projectExists(ProjectCode projectCode);

}
