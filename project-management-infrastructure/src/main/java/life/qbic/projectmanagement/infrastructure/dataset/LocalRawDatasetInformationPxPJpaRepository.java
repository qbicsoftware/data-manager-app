package life.qbic.projectmanagement.infrastructure.dataset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * <b>Local Raw Dataset Information PxP Jpa Repository</b>
 *
 * <p>Extension of the {@link PagingAndSortingRepository} interface to support
 * {@link LocalRawDatasetPxpEntry} entities.</p>
 *
 * @since 1.11.0
 */
public interface LocalRawDatasetInformationPxPJpaRepository extends
    PagingAndSortingRepository<LocalRawDatasetPxpEntry, String> {

  Page<LocalRawDatasetPxpEntry> findAllByExperimentId(String experimentId, Pageable pageable);
}
