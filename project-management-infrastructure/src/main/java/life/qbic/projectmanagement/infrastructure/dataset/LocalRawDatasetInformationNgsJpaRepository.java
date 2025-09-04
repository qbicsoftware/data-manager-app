package life.qbic.projectmanagement.infrastructure.dataset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface LocalRawDatasetInformationNgsJpaRepository extends
    PagingAndSortingRepository<LocalRawDatasetNgsEntry, String> {

  Page<LocalRawDatasetNgsEntry> findAllById(String id, Pageable pageable);

  Page<LocalRawDatasetNgsEntry> findAllByExperimentId(String experimentId, Pageable pageable);
}
