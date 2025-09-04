package life.qbic.projectmanagement.infrastructure.dataset;

import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface LocalRawDatasetInformationPxPJpaRepository extends
    PagingAndSortingRepository<LocalRawDatasetPxpEntry, String> {

  Page<LocalRawDatasetPxpEntry> findAllById(String id, Pageable pageable);

  Page<LocalRawDatasetPxpEntry> findAllByExperimentId(String experimentId, Pageable pageable);
}
