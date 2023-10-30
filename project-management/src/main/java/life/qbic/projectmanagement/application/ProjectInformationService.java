package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectPreviewLookup;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
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
  @PostFilter("hasPermission(filterObject.projectId(),'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public List<ProjectPreview> queryPreview(String filter, int offset, int limit,
      List<SortOrder> sortOrders) {
    // returned by JPA -> UnmodifiableRandomAccessList
    List<ProjectPreview> previewList = projectPreviewLookup.query(filter, offset, limit,
        sortOrders);
    // the list must be modifiable for spring security to filter it
    return new ArrayList<>(previewList);
  }

  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public Optional<Project> find(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    return projectRepository.find(projectId);
  }

  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public Optional<Project> find(String projectId) throws IllegalArgumentException{
    return find(ProjectId.parse(projectId));
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  private Project loadProject(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    log.debug("Search for project with id: " + projectId.value());
    return projectRepository.find(projectId).orElseThrow(() -> new ApplicationException(
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

  public void manageProject(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setProjectManager(contact);
    projectRepository.update(project);
  }

  public void investigateProject(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setPrincipalInvestigator(contact);
    projectRepository.update(project);
  }

  public void setResponsibility(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setResponsiblePerson(contact);
    projectRepository.update(project);
  }

  public void stateObjective(ProjectId projectId, String objective) {
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Project project = loadProject(projectId);
    project.stateObjective(projectObjective);
    projectRepository.update(project);
  }

  public void addFunding(ProjectId projectId, String label, String referenceId) {
    Funding funding = Funding.of(label, referenceId);
    var project = loadProject(projectId);
    project.setFunding(funding);
    projectRepository.update(project);

  }

  public void removeFunding(ProjectId projectId) {
    var project = loadProject(projectId);
    project.removeFunding();
    projectRepository.update(project);
  }
}
