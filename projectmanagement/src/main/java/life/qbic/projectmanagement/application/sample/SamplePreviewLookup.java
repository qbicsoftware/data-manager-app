package life.qbic.projectmanagement.application.sample;

import java.util.List;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

public interface SamplePreviewLookup {

  /**
   * Queries previews with a provided offset and limit that supports pagination.
   *
   * @param experimentId the {@link ExperimentId} for which the samplePreviews should be fetched
   * @param offset       the offset for the search result to start
   * @param limit        the maximum number of results that should be returned
   * @param sortOrders   the ordering to sort by
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<SamplePreview> queryByExperimentId(ExperimentId experimentId, int offset, int limit,
      List<SortOrder> sortOrders);

  /**
   * Queries the count of previews associated with a provided {@link ExperimentId}
   *
   * @param experimentId the {@link ExperimentId} for which the count of {@link SamplePreview}
   *                     should be found
   * @return the count of found {@link SamplePreview}
   * @since 1.0.0
   */
  int queryCountByExperimentId(ExperimentId experimentId);

}
