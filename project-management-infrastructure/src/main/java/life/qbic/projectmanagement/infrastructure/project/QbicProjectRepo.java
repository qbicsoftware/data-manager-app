package life.qbic.projectmanagement.infrastructure.project;

import java.util.Optional;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.repository.CrudRepository;

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

  Optional<Project> findProjectByProjectCode(ProjectCode projectCode);

}
