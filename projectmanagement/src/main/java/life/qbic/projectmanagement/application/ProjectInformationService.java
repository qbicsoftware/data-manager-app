package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
  public Optional<Project> find(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    return Optional.ofNullable(loadProject(projectId));
  }

  @PostAuthorize("hasPermission(returnObject,'VIEW_PROJECT')")
  private Project loadProject(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    log.debug("Search for project with id: " + projectId.value());
    return projectRepository.find(projectId).orElseThrow(() -> new ProjectManagementException(
            "Project with id" + projectId + "does not exit anymore")
        // should never happen; indicates dirty removal of project from db
    );
  }

  public void updateTitle(ProjectId projectId, String newTitle) {
    ProjectTitle projectTitle = ProjectTitle.of(newTitle);
    Project project = loadProject(projectId);
    project.updateTitle(projectTitle);
    projectRepository.update(project);
  }

  public void manageProject(ProjectId projectId, PersonReference personReference) {
    Project project = loadProject(projectId);
    project.setProjectManager(personReference);
    projectRepository.update(project);
  }

  public void investigateProject(ProjectId projectId, PersonReference personReference) {
    Project project = loadProject(projectId);
    project.setPrincipalInvestigator(personReference);
    projectRepository.update(project);
  }

  public void setResponsibility(ProjectId projectId, PersonReference personReference) {
    Project project = loadProject(projectId);
    project.setResponsiblePerson(personReference);
    projectRepository.update(project);
  }

  public void describeExperimentalDesign(ProjectId projectId, String experimentalDesign) {
    ExperimentalDesignDescription experimentalDesignDescription = ExperimentalDesignDescription.create(
        experimentalDesign);
    Project project = loadProject(projectId);
    project.describeExperimentalDesign(experimentalDesignDescription);
    projectRepository.update(project);
  }

  public void stateObjective(ProjectId projectId, String objective) {
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Project project = loadProject(projectId);
    project.stateObjective(projectObjective);
    projectRepository.update(project);
  }
}
