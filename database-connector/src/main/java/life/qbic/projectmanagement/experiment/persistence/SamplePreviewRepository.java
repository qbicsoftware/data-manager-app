package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  Page<SamplePreview> findSamplePreviewByExperimentId(ExperimentId experimentId, Pageable pageable,
      String filter);

  int countSamplePreviewsByExperimentId(ExperimentId experimentId);


}
