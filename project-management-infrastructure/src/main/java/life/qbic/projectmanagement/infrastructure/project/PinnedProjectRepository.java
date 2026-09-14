package life.qbic.projectmanagement.infrastructure.project;

import java.util.List;
import life.qbic.projectmanagement.application.pinned.PinnedProject;
import life.qbic.projectmanagement.application.pinned.PinnedProjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for the user-owned {@link PinnedProject} rows.
 * <p>
 * Queries are written as explicit JPQL because the primary key is an embeddable composite
 * ({@link PinnedProjectId}); property-path derived queries over an embedded id are brittle and would
 * silently depend on method naming.
 *
 * @since 1.12.0
 */
public interface PinnedProjectRepository extends
    JpaRepository<PinnedProject, PinnedProjectId> {

  @Query("""
      select pin from PinnedProject pin
      where pin.id.userId = :userId
      order by pin.pinnedAt desc, pin.id.projectId asc
      """)
  List<PinnedProject> findAllOfUser(@Param("userId") String userId);

  @Query("select count(pin) from PinnedProject pin where pin.id.userId = :userId")
  long countAllOfUser(@Param("userId") String userId);

  @Query("""
      select case when count(pin) > 0 then true else false end from PinnedProject pin
      where pin.id.userId = :userId and pin.id.projectId = :projectId
      """)
  boolean isPinned(@Param("userId") String userId, @Param("projectId") String projectId);

  @Query("""
      select pin from PinnedProject pin
      where pin.id.userId = :userId and pin.id.projectId = :projectId
      """)
  List<PinnedProject> findPin(@Param("userId") String userId,
      @Param("projectId") String projectId);

  @Modifying
  @Query("""
      delete from PinnedProject pin
      where pin.id.userId = :userId and pin.id.projectId = :projectId
      """)
  int removePin(@Param("userId") String userId, @Param("projectId") String projectId);
}
