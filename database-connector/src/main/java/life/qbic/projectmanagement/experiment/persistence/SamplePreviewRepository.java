package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Simple project preview repository to query concise project information
 *
 * @since 1.0.0
 */
public interface SamplePreviewRepository extends
    PagingAndSortingRepository<SamplePreview, SampleId> {

  Page<SamplePreview> findSamplePreviewByExperimentId(
      ExperimentId experimentId, Pageable pageable);

}
