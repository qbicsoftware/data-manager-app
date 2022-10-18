package life.qbic.projectmanagement.persistence;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * <b>Project JPA repository</b>
 *
 * <p>Implementation for the {@link ProjectRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with
 * persistent {@link Project} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicProjectRepo}, which is injected as
 * dependency
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
    if (doesProjectExistWithId(project.getId())) {
      throw new ProjectExistsException();
    }
    projectRepo.save(project);
  }

  @Override
  public void update(Project project) {
    if (!doesProjectExistWithId(project.getId())) {
      throw new ProjectNotFoundException();
    }
    projectRepo.save(project);
  }

  @Override
  public List<Project> find(ProjectCode projectCode) {
    return projectRepo.findProjectByProjectCode(projectCode);
  }

  @Override
  public Optional<Project> find(ProjectId projectId) {
    return projectRepo.findById(projectId);
  }

  private boolean doesProjectExistWithId(ProjectId id) {
    return projectRepo.findById(id).isPresent();
  }

  /**
   * Is thrown if a project that should be created already exists, as denoted by the project id
   */
  public static class ProjectExistsException extends RuntimeException {


    public ProjectExistsException() {
    }

    public ProjectExistsException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Thrown when a project is expected to exist but cannot be found.
   */
  public static class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException() {
    }

    public ProjectNotFoundException(Throwable cause) {
      super(cause);
    }
  }


}
