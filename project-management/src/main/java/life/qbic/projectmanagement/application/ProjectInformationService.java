package life.qbic.projectmanagement.application;

import static java.util.function.Predicate.not;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.SortOrder;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectContact;
import life.qbic.projectmanagement.application.api.ProjectOverviewLookup;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
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
 * Service that provides an API to search basic project information
 *
 * @since 1.0.0
 */
@Service
public class ProjectInformationService {

  private static final Logger log = LoggerFactory.logger(ProjectInformationService.class);
  private final ProjectOverviewLookup projectOverviewLookup;
  private final ProjectRepository projectRepository;
  private final ProjectAccessService projectAccessService;
  private final AuthenticationToUserIdTranslator userIdTranslator;

  public ProjectInformationService(@Autowired ProjectOverviewLookup projectOverviewLookup,
      @Autowired ProjectRepository projectRepository,
      @Autowired ProjectAccessService projectAccessService,
      AuthenticationToUserIdTranslator userIdTranslator) {
    Objects.requireNonNull(projectOverviewLookup);
    this.projectOverviewLookup = projectOverviewLookup;
    this.projectRepository = projectRepository;
    this.projectAccessService = projectAccessService;
    this.userIdTranslator = userIdTranslator;
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
    Optional<String> optionalUserId = userIdTranslator.translateToUserId(authentication);
    if (optionalUserId.isEmpty()) {
      return new ArrayList<>();
    }
    var accessibleProjectIds = projectAccessService.getAccessibleProjectsForSid(
        optionalUserId.get());
    List<ProjectId> accessibleProjectsFromRoles = authentication.getAuthorities().stream()
        .flatMap(it -> projectAccessService.getAccessibleProjectsForSid(
            it.getAuthority()).stream())
        .filter(not(accessibleProjectIds::contains))
        .toList();
    accessibleProjectIds.addAll(accessibleProjectsFromRoles);
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

  @PreAuthorize("hasPermission(#projectId,'life.qbic.projectmanagement.domain.model.project.Project','READ')")
  public Optional<ProjectOverview> findOverview(ProjectId projectId) {
    Objects.requireNonNull(projectId);
    return projectOverviewLookup.query("", 0, 1, List.of(), List.of(projectId)).stream()
        .findFirst();
  }

  public boolean isProjectCodeUnique(String projectCode) throws IllegalArgumentException {
    return !projectRepository.existsProjectByProjectCode(ProjectCode.parse(projectCode));
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
  public void manageProject(ProjectId projectId, ProjectContact contact) {
    var projectContact = new Contact(contact.fullName(), contact.email());
    manageProject(projectId, projectContact);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void investigateProject(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setPrincipalInvestigator(contact);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void investigateProject(ProjectId projectId, ProjectContact contact) {
    var projectContact = new Contact(contact.fullName(), contact.email());
    investigateProject(projectId, projectContact);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void setResponsibility(ProjectId projectId, Contact contact) {
    Project project = loadProject(projectId);
    project.setResponsiblePerson(contact);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void setResponsibility(ProjectId projectId, ProjectContact contact) {
    var projectContact = new Contact(contact.fullName(), contact.email());
    setResponsibility(projectId, projectContact);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void removeResponsibility(ProjectId projectId) {
    Project project = loadProject(projectId);
    project.removeResponsiblePerson();
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void updateObjective(ProjectId projectId, String objective) {
    ProjectObjective projectObjective = ProjectObjective.create(objective);
    Project project = loadProject(projectId);
    project.stateObjective(projectObjective);
    projectRepository.update(project);
  }

  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void setFunding(ProjectId projectId, String label, String referenceId) {
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
