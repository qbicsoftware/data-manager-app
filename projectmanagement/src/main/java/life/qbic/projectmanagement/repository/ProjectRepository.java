package life.qbic.projectmanagement.repository;

import life.qbic.projectmanagement.Project;
import life.qbic.projectmanagement.ProjectId;

import java.io.Serializable;
import java.util.Optional;

/**
 * <b> Provides stateless access and storage functionality for {@link Project} entities. </b>
 *
 * @since 1.0.0
 */
public class ProjectRepository implements Serializable {

  private static ProjectRepository INSTANCE;

  private final ProjectDataStorage dataStorage;

  /**
   * Retrieves a Singleton instance of a user {@link ProjectRepository}. In case this method is called
   * the first time, a new instance is created.
   *
   * @param dataStorage an implementation of {@link ProjectDataStorage}, handling the low level
   *                    persistence layer access.
   * @return a Singleton instance of a project repository.
   * @since 1.0.0
   */
  public static ProjectRepository getInstance(ProjectDataStorage dataStorage) {
    if (INSTANCE == null) {
      INSTANCE = new ProjectRepository(dataStorage);
    }
    return INSTANCE;
  }

  protected ProjectRepository(ProjectDataStorage dataStorage) {
    this.dataStorage = dataStorage;
  }

  /**
   * Searches for a project matching a provided projectId
   *
   * @param projectId the project's unique id, accessible via {@link Project#getId()}
   * @return the project if present in the repository, else returns an {@link Optional#empty()}.
   * @since 1.0.0
   */
  public Optional<Project> findById(ProjectId projectId) {
    return dataStorage.findProjectById(projectId);
  }

  /**
   * Adds a user to the repository. Publishes all domain events of the project if successful. If
   * unsuccessful, throws a {@link ProjectStorageException} Exception.
   *
   * @param project the project that shall be added to the repository
   * @throws ProjectStorageException if the project could not be added to the repository
   * @since 1.0.0
   */
  public void addProject(Project project) throws ProjectStorageException {
    saveProject(project);
  }

  private void saveProject(Project project) {
    try {
      if(doesProjectExistWithId(project.getId())) {
        throw new ProjectStorageException();
      }
      dataStorage.add(project);
    } catch (Exception e) {
      throw new ProjectStorageException(e);
    }
  }

  private boolean doesProjectExistWithId(ProjectId id) {
    return findById(id).isPresent();
  }

  public static class ProjectStorageException extends RuntimeException {


    public ProjectStorageException() {
    }

    public ProjectStorageException(Throwable cause) {
      super(cause);
    }
  }
}
