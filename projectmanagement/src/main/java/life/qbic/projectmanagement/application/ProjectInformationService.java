package life.qbic.projectmanagement.application;

import java.util.List;
import java.util.Objects;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic project information
 *
 * @since 1.0.0
 */
@Service
public class ProjectInformationService {

  private final ProjectPreviewLookup projectPreviewLookup;

  public ProjectInformationService(@Autowired ProjectPreviewLookup projectPreviewLookup) {
    Objects.requireNonNull(projectPreviewLookup);
    this.projectPreviewLookup = projectPreviewLookup;
  }

  /**
   * Queries {@link ProjectPreview}s with a provided offset and limit that supports pagination.
   *
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   * @since 1.0.0
   */
  public List<ProjectPreview> queryPreview(int offset, int limit) {
    return projectPreviewLookup.query(offset, limit);
  }

  /**
   * Queries {@link ProjectPreview}s with a provided offset and limit that supports pagination.
   *
   * @param filter the results' project title will be applied with this filter
   * @param offset the offset for the search result to start
   * @param limit  the maximum number of results that should be returned
   * @return the results in the provided range
   * @since 1.0.0
   */
  public List<ProjectPreview> queryPreview(String filter, int offset, int limit){
    return projectPreviewLookup.query(filter, offset, limit);
  }

}
