package life.qbic.projectmanagement.infrastructure.dataset.associated;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDataset;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.associated_dataset.repository.AssociatedDatasetRepository;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/**
 * Domain repository adapter implementation for {@link AssociatedDataset}
 * aggregates.
 *
 * <p>Wraps a Spring Data JPA repository ({@link AssociatedDatasetJpaRepository})
 * and delegates persistence. Exposes the same semantics as the domain
 * repository interface: soft-deleted connections are excluded from the
 * list/find queries by default (ADR-0001).</p>
 *
 * <p>The entity class ({@link AssociatedDataset}) lives in the
 * {@code domain/model/associated_dataset} package — this is the
 * codebase convention for aggregate roots (see {@link life.qbic.projectmanagement.domain.model.sample.Sample},
 * {@link life.qbic.projectmanagement.domain.model.experiment.Experiment}).</p>
 *
 * @since 1.12.0
 */
@Repository
public class AssociatedDatasetRepositoryImpl implements AssociatedDatasetRepository {

  private final AssociatedDatasetJpaRepository jpaRepository;

  public AssociatedDatasetRepositoryImpl(AssociatedDatasetJpaRepository jpaRepository) {
    this.jpaRepository = Objects.requireNonNull(jpaRepository,
        "jpaRepository must not be null");
  }

  @Override
  public void save(AssociatedDataset dataset) {
    Objects.requireNonNull(dataset, "dataset must not be null");
    jpaRepository.save(dataset);
  }

  @Override
  public List<AssociatedDataset> findByProject(ProjectId projectId) {
    Objects.requireNonNull(projectId, "projectId must not be null");
    // Default sort: newest connections first.
    // Future callers (e.g., UI column-header sorting) can extend this
    // by adding new methods to the domain repo interface.
    return jpaRepository.findActiveByProjectId(projectId, Sort.by(Sort.Direction.DESC, "connectedOn"));
  }

  @Override
  public Optional<AssociatedDataset> findById(AssociatedDatasetId id) {
    Objects.requireNonNull(id, "id must not be null");
    return jpaRepository.findById(id);
  }

  /**
   * Returns whether an actively-connected dataset with the given
   * external handle (same source type) already exists for the project.
   * Used to detect duplicates before attempting to connect.
   *
   * <p>This method is not part of the domain repository interface
   * because it's an infrastructure/optimization concern. It is exposed
   * here as an implementation-level hook available to the application
   * service when needed.</p>
   */
  public boolean isActiveConnectionPresent(
      ProjectId projectId, String externalHandleValue) {
    return jpaRepository.countActiveByProjectIdAndExternalHandle(
        projectId, externalHandleValue) > 0;
  }
}
