package life.qbic.projectmanagement.project.repository;

import life.qbic.projectmanagement.project.Project;
import life.qbic.projectmanagement.project.ProjectId;

import java.io.Serializable;

/**
 * <b> Provides stateless access and storage functionality for {@link Project} entities. </b>
 *
 * @since 1.0.0
 */
public class ProjectRepositoryToBeDeleted implements Serializable {

  private static ProjectRepositoryToBeDeleted INSTANCE;

  private final ProjectRepository dataStorage;

  /**
   * Retrieves a Singleton instance of a user {@link ProjectRepositoryToBeDeleted}. In case this method is called
   * the first time, a new instance is created.
   *
   * @param dataStorage an implementation of {@link ProjectRepository}, handling the low level
   *                    persistence layer access.
   * @return a Singleton instance of a project repository.
   * @since 1.0.0
   */
  public static ProjectRepositoryToBeDeleted getInstance(ProjectRepository dataStorage) {
    if (INSTANCE == null) {
      INSTANCE = new ProjectRepositoryToBeDeleted(dataStorage);
    }
    return INSTANCE;
  }

  protected ProjectRepositoryToBeDeleted(ProjectRepository dataStorage) {
    this.dataStorage = dataStorage;
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
    saveProjectIfNonexistent(project);
  }

  private void saveProjectIfNonexistent(Project project) {
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
      return dataStorage.findProjectById(id).isPresent();
    }

  public static class ProjectStorageException extends RuntimeException {


    public ProjectStorageException() {
    }

    public ProjectStorageException(Throwable cause) {
      super(cause);
    }
  }
}
