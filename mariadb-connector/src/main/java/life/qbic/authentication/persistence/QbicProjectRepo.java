package life.qbic.authentication.persistence;

import life.qbic.projectmanagement.Project;
import life.qbic.projectmanagement.ProjectTitle;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
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
public interface QbicProjectRepo extends CrudRepository<Project, ProjectTitle> {

  /**
   * Find projects by title in the persistent data storage
   *
   * @param projectTitle the title to filter projects for
   * @return a list of matching project that have the given title
   * @since 1.0.0
   */
  List<Project> findProjectsByTitle(ProjectTitle projectTitle);

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
