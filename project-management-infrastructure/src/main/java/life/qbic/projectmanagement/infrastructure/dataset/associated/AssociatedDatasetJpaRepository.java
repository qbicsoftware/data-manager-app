package life.qbic.projectmanagement.infrastructure.dataset.associated;

import java.util.List;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDataset;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link AssociatedDataset} entities.
 *
 * <p>Default queries exclude soft-deleted (REMOVED) connections per
 * ADR-0001.
 *
 * @since 1.12.0
 */
interface AssociatedDatasetJpaRepository
    extends JpaRepository<AssociatedDataset, AssociatedDatasetId> {

  /**
   * Returns all connected datasets for a project, excluding REMOVED
   * (tombstone) rows. This is the standard query used for listing
   * active connections in the datasets view.
   *
   * <p>Ordering is driven by the {@code Sort} parameter — the caller
   * ({@link AssociatedDatasetRepositoryImpl}) picks the default sort.
   * This keeps the query open for future ordering needs (e.g. by title
   * or publication date) without changing the JPQL.</p>
   */
  @Query("SELECT d FROM associated_dataset d "
      + "WHERE d.projectId = :projectId "
      + "AND d.connectionState <> life.qbic.projectmanagement.domain.model.associated_dataset.ConnectionState.REMOVED")
  List<AssociatedDataset> findActiveByProjectId(
      @Param("projectId") ProjectId projectId,
      Sort sort);

  /**
   * Checks whether a dataset with the given PID (persistent identifier,
   * e.g. DOI) is already actively connected to the project. Used to
   * prevent duplicate connections to the same logical record.
   *
   * <p>PIDs are globally unique by design, so no source-type filter is
   * needed — a DOI from Zenodo and the same DOI from another InvenioRDM
   * instance refer to the same logical record.</p>
   */
  @Query("SELECT COUNT(d) FROM associated_dataset d WHERE "
      + "d.projectId = :projectId "
      + "AND d.pid = :pid "
      + "AND d.connectionState <> life.qbic.projectmanagement.domain.model.associated_dataset.ConnectionState.REMOVED")
  long countActiveByProjectIdAndPid(
      @Param("projectId") ProjectId projectId,
      @Param("pid") String pid);
}
