package life.qbic.projectmanagement.application;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.domain.project.OfferIdentifier;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic project information
 *
 * @since 1.0.0
 */
@Service
public class ProjectInformationService {

  private static final Logger log = LoggerFactory.logger(ProjectInformationService.class);
  private final ProjectPreviewLookup projectPreviewLookup;

  private final ProjectRepository projectRepository;

  public ProjectInformationService(@Autowired ProjectPreviewLookup projectPreviewLookup,
      @Autowired ProjectRepository projectRepository) {
    Objects.requireNonNull(projectPreviewLookup);
    this.projectPreviewLookup = projectPreviewLookup;
    this.projectRepository = projectRepository;
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
  public List<ProjectPreview> queryPreview(String filter, int offset, int limit) {
    return projectPreviewLookup.query(filter, offset, limit);
  }

  public Optional<Project> find(ProjectId projectId) {
    log.debug("Search for project with id: " + projectId.toString());
    return projectRepository.find(projectId);
  }

  public List<OfferIdentifier> queryLinkedOffers(ProjectId projectId) {
    return projectRepository.find(projectId).map(Project::linkedOffers).orElse(
        Collections.emptyList());
  }


}
