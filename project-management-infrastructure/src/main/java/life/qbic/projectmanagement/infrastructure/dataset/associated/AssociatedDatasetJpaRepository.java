package life.qbic.projectmanagement.infrastructure.dataset.associated;

import java.util.List;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDataset;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.associated_dataset.ConnectionState;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link AssociatedDataset} entities.
 *
 * <p>Default queries exclude soft-deleted (REMOVED) connections per
 * ADR-0001. Use {@link #findByProjectIdExcludingState} with the
 * {@link ConnectionState#REMOVED} state to get active-only rows.</p>
 *
 * @since 1.12.0
 */
interface AssociatedDatasetJpaRepository
    extends JpaRepository<AssociatedDataset, AssociatedDatasetId> {

  /**
   * Returns all connected datasets for a project, excluding REMOVED
   * (tombstone) rows. This is the standard query used for listing
   * active connections in the datasets view.
   */
  @Query("SELECT d FROM associated_dataset d "
      + "WHERE d.projectId = :projectId "
      + "AND d.connectionState <> life.qbic.projectmanagement.domain.model.associated_dataset.ConnectionState.REMOVED")
  List<AssociatedDataset> findActiveByProjectId(@Param("projectId") ProjectId projectId);

  /**
   * Checks whether a dataset with the given external handle (for the
   * same source type) is already connected (active) to the project.
   * Used to detect duplicates before connecting.
   */
  @Query("SELECT COUNT(d) FROM associated_dataset d WHERE "
      + "d.projectId = :projectId "
      + "AND d.externalHandle = :externalHandle "
      + "AND d.sourceType = life.qbic.projectmanagement.domain.model.associated_dataset.SourceType.INVENIO_RDM "
      + "AND d.connectionState <> life.qbic.projectmanagement.domain.model.associated_dataset.ConnectionState.REMOVED")
  long countActiveByProjectIdAndExternalHandle(
      @Param("projectId") ProjectId projectId,
      @Param("externalHandle") String externalHandle);
}
