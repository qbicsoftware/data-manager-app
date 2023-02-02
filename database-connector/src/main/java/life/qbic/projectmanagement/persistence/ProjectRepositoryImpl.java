package life.qbic.projectmanagement.persistence;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectCreationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * <b>Project repository implementation</b>
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
 * Also handles project storage in openBIS through {@link QbicProjectDataRepo}
 *
 * @since 1.0.0
 */
@Component
@Scope("singleton")
public class ProjectRepositoryImpl implements ProjectRepository {

  private static final Logger log = logger(ProjectRepositoryImpl.class);
  private final QbicProjectRepo projectRepo;
  private final QbicProjectDataRepo projectDataRepo;

  @Autowired
  public ProjectRepositoryImpl(QbicProjectRepo projectRepo, QbicProjectDataRepo projectDataRepo) {
    this.projectRepo = projectRepo;
    this.projectDataRepo = projectDataRepo;
  }

  @Override
  public void add(Project project) {
    ProjectCode projectCode = project.getProjectCode();
    if (doesProjectExistWithId(project.getId()) || projectDataRepo.projectExists(projectCode)) {
      throw new ProjectExistsException();
    }
    projectRepo.save(project);

    try {
      projectDataRepo.add(project.getProjectCode());
    } catch (Exception e) {
      log.error("Could not add project to openBIS. Removing project from repository, as well.");
      projectRepo.delete(project);
      throw e;
    }
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
