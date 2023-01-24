package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.PersonReference;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
import life.qbic.projectmanagement.domain.project.repository.ExperimentalDesignVocabularyRepository;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
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
      @Autowired ProjectRepository projectRepository,
      @Autowired ExperimentalDesignVocabularyRepository experimentalDesignVocabularyRepository) {
    Objects.requireNonNull(projectPreviewLookup);
    this.projectPreviewLookup = projectPreviewLookup;
    this.projectRepository = projectRepository;
  }

  /**
   * Queries {@link ProjectPreview}s with a provided offset and limit that supports pagination.
   *
   * @param filter     the results' project title will be applied with this filter
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   * @since 1.0.0
   */
  @PostFilter("hasPermission(filterObject,'VIEW_PROJECT')")
  public List<ProjectPreview> queryPreview(String filter, int offset, int limit,
      List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<ProjectPreview> previewList = projectPreviewLookup.query(filter, offset, limit,
        sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(previewList);
  }

  public Optional<Project> find(ProjectId projectId) {
    log.debug("Search for project with id: " + projectId.toString());
    return projectRepository.find(projectId);
  }

  public void updateTitle(String projectId, String newTitle) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectTitle projectTitle = ProjectTitle.of(newTitle);
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.updateTitle(projectTitle);
      projectRepository.update(p);
    });
  }

  public void manageProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.setProjectManager(personReference);
      projectRepository.update(p);
    });
  }

  public void investigateProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.setPrincipalInvestigator(personReference);
      projectRepository.update(p);
    });
  }

  public void describeExperimentalDesign(String projectId, String experimentalDesign) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ExperimentalDesignDescription experimentalDesignDescription = ExperimentalDesignDescription.create(
        experimentalDesign);
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.describeExperimentalDesign(experimentalDesignDescription);
      projectRepository.update(p);
    });
  }

  public void stateObjective(String projectId, String objective) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Optional<Project> project = projectRepository.find(projectIdentifier);
    project.ifPresent(p -> {
      p.stateObjective(projectObjective);
      projectRepository.update(p);
    });
  }


}
