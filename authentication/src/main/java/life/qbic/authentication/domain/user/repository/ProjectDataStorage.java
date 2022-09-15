package life.qbic.authentication.domain.user.repository;

import life.qbic.projectmanagement.Project;
import life.qbic.projectmanagement.ProjectId;

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

  Optional<Project> findProjectById(ProjectId projectId);
}
