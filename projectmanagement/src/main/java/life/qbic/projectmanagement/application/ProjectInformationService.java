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
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
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
      @Autowired ProjectRepository projectRepository) {
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

  @PostAuthorize("hasPermission(returnObject,'VIEW_PROJECT')")
  public Optional<Project> find(String projectId) {
    Objects.requireNonNull(projectId);
    log.debug("Search for project with id: " + projectId);
    return projectRepository.find(ProjectId.parse(projectId));
  }

  @PostAuthorize("hasPermission(returnObject,'VIEW_PROJECT')")
  private Project loadProject(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    return projectRepository.find(projectId).orElseThrow(() -> new ProjectManagementException(
            "Project with id" + projectId.toString() + "does not exit anymore")
        // should never happen; indicates dirty removal of project from db
    );
  }

  public void updateTitle(String projectId, String newTitle) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectTitle projectTitle = ProjectTitle.of(newTitle);
    Project project = loadProject(projectIdentifier);
    project.updateTitle(projectTitle);
    projectRepository.update(project);
  }

  public void manageProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Project project = loadProject(projectIdentifier);
    project.setProjectManager(personReference);
    projectRepository.update(project);
  }

  public void investigateProject(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Project project = loadProject(projectIdentifier);
    project.setPrincipalInvestigator(personReference);
    projectRepository.update(project);
  }

  public void setResponsibility(String projectId, PersonReference personReference) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    Project project = loadProject(projectIdentifier);
    project.setResponsiblePerson(personReference);
    projectRepository.update(project);
  }

  public void describeExperimentalDesign(String projectId, String experimentalDesign) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ExperimentalDesignDescription experimentalDesignDescription = ExperimentalDesignDescription.create(
        experimentalDesign);
    Project project = loadProject(projectIdentifier);
    project.describeExperimentalDesign(experimentalDesignDescription);
    projectRepository.update(project);
  }

  public void stateObjective(String projectId, String objective) {
    ProjectId projectIdentifier = ProjectId.of(UUID.fromString(projectId));
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Project project = loadProject(projectIdentifier);
    project.stateObjective(projectObjective);
    projectRepository.update(project);
  }
}
