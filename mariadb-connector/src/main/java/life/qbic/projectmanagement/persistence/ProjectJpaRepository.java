package life.qbic.projectmanagement.persistence;

import life.qbic.OffsetBasedRequest;
import life.qbic.projectmanagement.domain.project.ProjectPreview;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


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

  private final ProjectPreviewRepository projectPreviewRepo;

  @Autowired
  public ProjectJpaRepository(QbicProjectRepo projectRepo, ProjectPreviewRepository projectPreviewRepo) {
    this.projectRepo = projectRepo;
    this.projectPreviewRepo = projectPreviewRepo;
  }

  @Override
  public void add(Project project) {
    if(doesProjectExistWithId(project.getId())) {
      throw new ProjectExistsException();
    }
    projectRepo.save(project);
  }

  @Override
  public List<ProjectPreview> getAllPreviews(int offset, int limit) {
    return projectPreviewRepo.findAll(new OffsetBasedRequest(offset, limit)).getContent();
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

}
