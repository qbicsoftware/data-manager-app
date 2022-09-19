package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.project.repository.ProjectRepository;
import life.qbic.projectmanagement.project.Project;
import life.qbic.projectmanagement.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
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
@Scope("singleton")
public class ProjectJpaRepository implements ProjectRepository {

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
