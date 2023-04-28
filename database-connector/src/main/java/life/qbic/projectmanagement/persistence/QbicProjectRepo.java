package life.qbic.projectmanagement.persistence;

import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

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

  List<Project> findProjectByProjectCode(ProjectCode projectCode);
}
