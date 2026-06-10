package life.qbic.projectmanagement.infrastructure.dataset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * <b>Local Raw Dataset Information IP Jpa Repository</b>
 *
 * <p>Extension of the {@link PagingAndSortingRepository} interface to support
 * {@link LocalRawDatasetIpEntry} entities.</p>
 *
 * @since 1.12.0
 */
public interface LocalRawDatasetInformationIpJpaRepository extends
    PagingAndSortingRepository<LocalRawDatasetIpEntry, String>,
    JpaSpecificationExecutor<LocalRawDatasetIpEntry> {

  Page<LocalRawDatasetIpEntry> findAllByExperimentId(String experimentId, Pageable pageable);
}
