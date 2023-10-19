package life.qbic.controlling.application.api;

import java.util.List;
import life.qbic.controlling.application.ProjectPreview;
import life.qbic.controlling.application.SortOrder;

public interface ProjectPreviewLookup {

  /**
   * Queries previews with a provided offset and limit that supports pagination.
   *
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<ProjectPreview> query(int offset, int limit);

  /**
   * Queries previews with a provided offset and limit that supports pagination.
   *
   * @param filter     the results' project title will be applied with this filter
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the ordering to sort by
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<ProjectPreview> query(String filter, int offset, int limit, List<SortOrder> sortOrders);

}
