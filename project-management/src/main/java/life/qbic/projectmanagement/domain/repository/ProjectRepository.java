package life.qbic.projectmanagement.domain.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;


/**
 * <b>Project Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link Project} data storage.
 *
 * @since 1.0.0
 */
public interface ProjectRepository {

  /**
   * Saves a {@link Project} entity permanently.
   *
   * @param project the project to store
   * @since 1.0.0
   */
  void add(Project project);

  /**
   * Updates a {@link Project} entity.
   *
   * @param project the project to update
   * @since 1.0.0
   */
  void update(Project project);

  /**
   * Searches for projects that contain the provided project code
   *
   * @param projectCode the project code to search for in projects
   * @return projects that contain the project code
   * @since 1.0.0
   */
  List<Project> find(ProjectCode projectCode);

  Optional<Project> find(ProjectId projectId);

  /**
   * Is thrown if a project that should be created already exists, as denoted by the project id
   */
  class ProjectExistsException extends RuntimeException {


    public ProjectExistsException() {
    }

    public ProjectExistsException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Thrown when a project is expected to exist but cannot be found.
   */
  class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException() {
    }

    public ProjectNotFoundException(Throwable cause) {
      super(cause);
    }
  }
}
