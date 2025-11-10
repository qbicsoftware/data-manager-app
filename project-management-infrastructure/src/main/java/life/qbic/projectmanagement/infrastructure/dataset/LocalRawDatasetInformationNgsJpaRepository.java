package life.qbic.projectmanagement.infrastructure.dataset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * <b>Local Raw Dataset Information NGS Jpa Repository</b>
 *
 * <p>Extension of the {@link PagingAndSortingRepository} interface to support
 * {@link LocalRawDatasetNgsEntry} entities.</p>
 *
 * @since 1.11.0
 */
public interface LocalRawDatasetInformationNgsJpaRepository extends
    PagingAndSortingRepository<LocalRawDatasetNgsEntry, String>, JpaSpecificationExecutor<LocalRawDatasetNgsEntry> {

  Page<LocalRawDatasetNgsEntry> findAllByExperimentId(String experimentId, Pageable pageable);
}
