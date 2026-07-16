package life.qbic.projectmanagement.domain.model.associated_dataset.repository;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDataset;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Repository for {@link AssociatedDataset} aggregates.
 *
 * <p>Implementations must only return aggregates in active (non-removed)
 * state by default. Removed connections are retained in storage as audit
 * tombstones but are not surfaced through the standard query methods.</p>
 *
 * @since 1.12.0
 */
public interface AssociatedDatasetRepository {

  /**
   * Persists a newly connected dataset.
   *
   * @throws IllegalArgumentException if the aggregate is already in REMOVED state
   */
  void save(AssociatedDataset dataset);

  /**
   * Finds all actively connected datasets for a project (excludes REMOVED).
   */
  List<AssociatedDataset> findByProject(ProjectId projectId);

  /**
   * Finds a dataset connection by its identifier.
   */
  Optional<AssociatedDataset> findById(AssociatedDatasetId id);

}
