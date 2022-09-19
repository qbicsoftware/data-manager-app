package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.project.repository.ProjectRepository;
import life.qbic.projectmanagement.project.Project;
import life.qbic.projectmanagement.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * <b>Project JPA repository</b>
 *
 * <p>Implementation for the {@link ProjectRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact with
 * persistent {@link Project} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicProjectRepo}, which is injected as dependency
 * upon creation.
 *
 * @since 1.0.0
 */
@Component
public class ProjectJpaRepository implements ProjectRepository {

  private static ProjectJpaRepository INSTANCE;
  private final QbicProjectRepo projectRepo;

  @Autowired
  public ProjectJpaRepository(QbicProjectRepo projectRepo) {
    this.projectRepo = projectRepo;
  }

  @Override
  public void add(Project project) {
    saveProjectIfNonexistent(project);
  }

  @Override
  public Optional<Project> findProjectById(ProjectId projectId) {
    return projectRepo.findById(projectId);
  }


  /**
   * Retrieves a Singleton instance of a user {@link ProjectJpaRepository}. In case this method is called
   * the first time, a new instance is created.
   *
   * @param projectRepo an implementation of {@link QbicProjectRepo}, handling the low level
   *                    persistence layer access.
   * @return a Singleton instance of a project repository.
   * @since 1.0.0
   */
  public static ProjectJpaRepository getInstance(QbicProjectRepo projectRepo) {
    if (INSTANCE == null) {
      INSTANCE = new ProjectJpaRepository(projectRepo);
    }
    return INSTANCE;
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
      projectRepo.save(project);
    } catch (Exception e) {
      throw new ProjectStorageException(e);
    }
  }

  private boolean doesProjectExistWithId(ProjectId id) {
    return projectRepo.findById(id).isPresent();
  }

  public static class ProjectStorageException extends RuntimeException {


    public ProjectStorageException() {
    }

    public ProjectStorageException(Throwable cause) {
      super(cause);
    }
  }

}
