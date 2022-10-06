package life.qbic.projectmanagement.application.api;

import java.util.List;
import life.qbic.projectmanagement.application.ProjectPreview;

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
   * @param filter the results' project title will be applied with this filter
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   * @since 1.0.0
   */
  List<ProjectPreview> query(String filter, int offset, int limit);

}
