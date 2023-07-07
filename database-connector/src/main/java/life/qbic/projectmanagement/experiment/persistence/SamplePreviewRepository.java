package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.project.sample.Sample;
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
