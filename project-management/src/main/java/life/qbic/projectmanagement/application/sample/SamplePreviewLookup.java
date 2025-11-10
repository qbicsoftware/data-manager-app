package life.qbic.projectmanagement.application.sample;

import java.util.List;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewFilter;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;

public interface SamplePreviewLookup {

  /**
   * Queries previews with a provided offset and limit that supports pagination.
   *
   * @param experimentId the {@link ExperimentId} for which the samplePreviews should be fetched
   * @param offset       the offset for the search result to start
   * @param limit        the maximum number of results that should be returned
   * @param sortOrders   the ordering to sort by
   * @param filter       the results fields will be checked for the value within this filter
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<SamplePreview> queryByExperimentId(ExperimentId experimentId, int offset, int limit,
      List<SortOrder> sortOrders, String filter);

  List<SamplePreview> queryByExperimentId(ExperimentId experimentId);

  /**
   * Queries the count of previews associated with a provided {@link ExperimentId}
   *
   * @param experimentId the {@link ExperimentId} for which the count of {@link SamplePreview}
   *                     should be found
   * @param filter       the results fields will be checked for the value within this filter
   * @return the count of found {@link SamplePreview}
   * @since 1.0.0
   */
  int queryCountByExperimentId(ExperimentId experimentId, String filter);

  List<SamplePreview> queryByExperimentId(String experimentId, int offset, int limit, SamplePreviewFilter filter) throws LookupException;

  class LookupException extends RuntimeException {

    public LookupException(String message) {
      super(message);
    }

    public LookupException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
