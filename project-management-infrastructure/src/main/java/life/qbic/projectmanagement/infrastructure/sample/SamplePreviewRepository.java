package life.qbic.projectmanagement.infrastructure.sample;

import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Simple sample preview repository to search concise {@link Sample} information
 *
 * @since 1.0.0
 */
@Repository
public interface SamplePreviewRepository extends
    JpaRepository<SamplePreview, Long>, JpaSpecificationExecutor<SamplePreview> {

}
