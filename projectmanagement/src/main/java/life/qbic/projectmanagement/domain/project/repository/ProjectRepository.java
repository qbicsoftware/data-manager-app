package life.qbic.projectmanagement.domain.project.repository;

import life.qbic.projectmanagement.domain.project.Project;

import java.util.List;


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
   * Returns all stored {@link Project} entities.
   *
   * @return a list of projects
   */
  List<Project> getAll();
}
