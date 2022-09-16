package life.qbic.projectmanagement.project.repository;

import life.qbic.projectmanagement.project.Project;
import life.qbic.projectmanagement.project.ProjectId;

import java.util.Optional;


/**
 * <b>Project Data Storage Interface</b>
 *
 * <p>Provides access to the persistence layer that handles the {@link Project} data storage.
 *
 * @since 1.0.0
 */
public interface ProjectDataStorage {

  /**
   * Saves a {@link Project} entity permanently.
   *
   * @param project the project to store
   * @since 1.0.0
   */
  void add(Project project);

  /**
   * Searches for a project matching a provided projectId
   *
   * @param projectId the project's unique id, accessible via {@link Project#getId()}
   * @return the project if present in the repository, else returns an {@link Optional#empty()}.
   * @since 1.0.0
   */
  Optional<Project> findProjectById(ProjectId projectId);
}
