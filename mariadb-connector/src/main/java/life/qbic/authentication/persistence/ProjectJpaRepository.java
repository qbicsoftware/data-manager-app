package life.qbic.authentication.persistence;

import life.qbic.authentication.domain.user.repository.ProjectDataStorage;
import life.qbic.projectmanagement.Project;
import life.qbic.projectmanagement.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * <b>Project JPA repository</b>
 *
 * <p>Implementation for the {@link ProjectDataStorage} interface.
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
public class ProjectJpaRepository implements ProjectDataStorage {

  private final QbicProjectRepo projectRepo;

  @Autowired
  public ProjectJpaRepository(QbicProjectRepo projectRepo) {
    this.projectRepo = projectRepo;
  }

  @Override
  public void add(Project project) {
    projectRepo.save(project);
  }

  @Override
  public Optional<Project> findProjectById(ProjectId projectId) {
    return projectRepo.findProjectById(projectId);
  }
}
