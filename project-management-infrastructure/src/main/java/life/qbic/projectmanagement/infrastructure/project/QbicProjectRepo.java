package life.qbic.projectmanagement.infrastructure.project;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

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

  boolean existsProjectByProjectCode(ProjectCode projectCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Project p where p.projectId = :id")
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "15000")) // ms
  Optional<Project> findByIdForUpdate(@Param("id") ProjectId id);
}
