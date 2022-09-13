package life.qbic.authentication.domain.user.repository;

import life.qbic.authentication.domain.user.concept.User;
import life.qbic.projectmanagement.ProjectTitle;
import life.qbic.projectmanagement.Project;

import java.util.List;
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
   * Searches for any available project entities matching the title (or parts of it).
   *
   * <p>Note, that the implementation must not make any assumptions by number of occurrences.
   * The implementation shall return any project and leave the logic to the application layer.
   *
   * @param projectTitle the project title to use as search filter
   * @return a list of matching {@link Project} entries. Is empty, if no matching project is present matching the title
   * @since 1.0.0
   */
  List<Project> findProjectsByTitle(ProjectTitle projectTitle);

  /**
   * Saves a {@link Project} entity permanently.
   *
   * @param project the project to store
   * @since 1.0.0
   */
  void save(Project project);

  Optional<Project> findProjectById(ProjectId projectId);
}
