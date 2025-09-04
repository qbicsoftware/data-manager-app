package life.qbic.projectmanagement.infrastructure.dataset;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>Local Raw Dataset Jpa Repository</b>
 *
 * <p>Extension of the {@link JpaRepository} to support {@link LocalRawDatasetEntry entities.}</p>
 *
 * @since 1.11.0
 */
public interface LocalRawDatasetJpaRepository extends JpaRepository<LocalRawDatasetEntry, String> {

}
