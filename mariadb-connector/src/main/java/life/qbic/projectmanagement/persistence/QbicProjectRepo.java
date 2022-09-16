package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.project.Project;
import life.qbic.projectmanagement.project.ProjectId;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

/**
 * <b>QBiC project repository interface</b>
 *
 * <p>This interface will be automatically detected by Spring on application startup and create an
 * instance of this class automatically.
 *
 * <p>Since it extends the {@link CrudRepository} class from Spring, no need to write queries. The
 * framework will do that for us.
 *
 * @since 1.0.0
 */
public interface QbicProjectRepo extends CrudRepository<Project, ProjectId> {

  /**
   * Find project by mail address in the persistent data storage
   *
   * @param projectId the id to filter projects for
   * @return a project object, or {@link Optional#empty()} if no entity with the provided id was
   * found.
   * @since 1.0.0
   */
  Optional<Project> findProjectById(ProjectId projectId);
}
