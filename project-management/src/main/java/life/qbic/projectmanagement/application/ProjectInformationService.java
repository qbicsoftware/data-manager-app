package life.qbic.projectmanagement.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.SortOrder;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectOverviewLookup;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.authorities.Role;
import life.qbic.projectmanagement.domain.model.project.Contact;
import life.qbic.projectmanagement.domain.model.project.Funding;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Service that provides an API to query basic project information
 *
 * @since 1.0.0
 */
@Service
public class ProjectInformationService {

  private static final Logger log = LoggerFactory.logger(ProjectInformationService.class);
  private final ProjectOverviewLookup projectOverviewLookup;
  private final ProjectRepository projectRepository;
  private final ProjectAccessService projectAccessService;

  public ProjectInformationService(@Autowired ProjectOverviewLookup projectOverviewLookup,
      @Autowired ProjectRepository projectRepository,
      @Autowired ProjectAccessService projectAccessService) {
    Objects.requireNonNull(projectOverviewLookup);
    this.projectOverviewLookup = projectOverviewLookup;
    this.projectRepository = projectRepository;
    this.projectAccessService = projectAccessService;
  }

  /**
   * Queries {@link ProjectOverview}s with a provided offset and limit that supports pagination.
   *
   * @param filter     the results' project title will be applied with this filter
   * @param offset     the offset for the search result to start
   * @param limit      the maximum number of results that should be returned
   * @param sortOrders the sort orders to apply
   * @return the results in the provided range
   * @since 1.0.0
   */
  public List<ProjectOverview> queryOverview(String filter, int offset, int limit,
      List<SortOrder> sortOrders) {
    var accessibleProjectIds = retrieveAccessibleProjectIdsForUser();
    return projectOverviewLookup.query(filter, offset, limit,
        sortOrders, accessibleProjectIds);
  }

  /* @PostFilter() annotation is not possible for acl secured objects in a paginated context, for more details see:
     https://github.com/spring-projects/spring-security/issues/2629
     therefore the list of accessible projectIds for the user have to be retrieved beforehand
   */
  private List<ProjectId> retrieveAccessibleProjectIdsForUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var principal = ((QbicUserDetails) authentication.getPrincipal());
    var userId = principal.getUserId();
    var userRole = principal.getAuthorities().stream()
        .filter(grantedAuthority -> grantedAuthority instanceof Role).findFirst();
    var accessibleProjectIds = projectAccessService.getAccessibleProjectsForSid(userId);
    userRole.ifPresent(grantedAuthority -> accessibleProjectIds.addAll(
        projectAccessService.getAccessibleProjectsForSid(grantedAuthority.getAuthority())));
    return accessibleProjectIds;
  }

  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public Optional<Project> find(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    return projectRepository.find(projectId);
  }

  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public Optional<Project> find(String projectId) throws IllegalArgumentException {
    return find(ProjectId.parse(projectId));
  }

  public boolean isProjectCodeUnique(String projectCode) throws IllegalArgumentException {
    return projectRepository.find(ProjectCode.parse(projectCode)).isEmpty();
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  private Project loadProject(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    log.debug("Search for project with id: " + projectId.value());
    return projectRepository.find(projectId).orElseThrow(() -> new ApplicationException(
            "Project with id" + projectId + "does not exist anymore")
        // should never happen; indicates dirty removal of project from db
    );
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void updateTitle(ProjectId projectId, String newTitle) {
    ProjectTitle projectTitle = ProjectTitle.of(newTitle);
    Project project = loadProject(projectId);
    project.updateTitle(projectTitle);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void manageProject(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setProjectManager(contact);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void investigateProject(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setPrincipalInvestigator(contact);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void setResponsibility(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setResponsiblePerson(contact);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void stateObjective(ProjectId projectId, String objective) {
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Project project = loadProject(projectId);
    project.stateObjective(projectObjective);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void addFunding(ProjectId projectId, String label, String referenceId) {
    Funding funding = Funding.of(label, referenceId);
    var project = loadProject(projectId);
    project.setFunding(funding);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void removeFunding(ProjectId projectId) {
    var project = loadProject(projectId);
    project.removeFunding();
    projectRepository.update(project);
  }

  public void updateModifiedDate(ProjectId projectID, Instant modifiedOn) {
    projectRepository.unsafeUpdateLastModified(projectID, modifiedOn);
  }
}
