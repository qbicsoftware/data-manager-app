package life.qbic.projectmanagement.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
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

  public void updateTitle(String projectId, String newTitle) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    ProjectTitle projectTitle = ProjectTitle.of(newTitle);
    project.ifPresent(p -> {
      p.getProjectIntent().projectTitle(projectTitle);
      p.setLastModified(Instant.now());
      projectRepository.update(p);
    });
  }

  public void describeExperimentalDesign(String projectId, String experimentalDesign) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    ExperimentalDesignDescription experimentalDesignDescription = ExperimentalDesignDescription.create(
        experimentalDesign);
    project.ifPresent(p -> {
      p.getProjectIntent().experimentalDesign(experimentalDesignDescription);
      p.setLastModified(Instant.now());
      projectRepository.update(p);
    });
  }

  public void stateObjective(String projectId, String objective) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    project.ifPresent(p -> {
      p.getProjectIntent().objective(projectObjective);
      p.setLastModified(Instant.now());
      projectRepository.update(p);
    });
  }


}
