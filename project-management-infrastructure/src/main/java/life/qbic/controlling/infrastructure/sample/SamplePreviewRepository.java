package life.qbic.controlling.infrastructure.sample;

import life.qbic.controlling.application.sample.SamplePreview;
import life.qbic.controlling.domain.model.sample.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Simple sample preview repository to query concise {@link Sample} information
 *
 * @since 1.0.0
 */
@Repository
public interface SamplePreviewRepository extends
    JpaRepository<SamplePreview, Long>, JpaSpecificationExecutor<SamplePreview> {

}
