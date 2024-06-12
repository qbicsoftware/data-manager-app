package life.qbic.projectmanagement.infrastructure.project;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.application.authorization.authorities.aspects.CanCreateProject;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * <b>Project repository implementation</b>
 *
 * <p>Implementation for the {@link ProjectRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link Project} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicProjectRepo}, which is injected as
 * dependency upon creation.
 * <p>
 * Also handles project storage in openBIS through {@link QbicProjectDataRepo}
 *
 * @since 1.0.0
 */
@Component
public class ProjectRepositoryImpl implements ProjectRepository {

  private static final Logger log = logger(ProjectRepositoryImpl.class);
  private final QbicProjectRepo projectRepo;
  private final QbicProjectDataRepo projectDataRepo;
  private final ProjectAccessService projectAccessService;

  @Autowired
  public ProjectRepositoryImpl(QbicProjectRepo projectRepo,
      QbicProjectDataRepo projectDataRepo, ProjectAccessService projectAccessService) {
    this.projectRepo = projectRepo;
    this.projectDataRepo = projectDataRepo;
    this.projectAccessService = projectAccessService;
  }

  @Override
  @CanCreateProject
  @Transactional
  public void add(Project project) {
    ProjectCode projectCode = project.getProjectCode();
    if (doesProjectExistWithId(project.getId()) || projectDataRepo.projectExists(projectCode)) {
      throw new ProjectExistsException();
    }
    var savedProject = projectRepo.save(project);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = ((QbicUserDetails) authentication.getPrincipal()).getUserId();
    projectAccessService.initializeProject(savedProject.getId(), userId);
    projectAccessService.addAuthorityAccess(savedProject.getId(),
        "ROLE_ADMIN", ProjectAccessService.ProjectRole.ADMIN);
    projectAccessService.addAuthorityAccess(savedProject.getId(), "ROLE_PROJECT_MANAGER",
        ProjectRole.ADMIN);
    try {
      projectDataRepo.add(project);
    } catch (Exception e) {
      log.error("Could not add project to openBIS. Removing project from repository, as well.");
      projectRepo.delete(project);
      throw e;
    }
  }

  @Override
  @PreAuthorize("hasPermission(#project.id, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void update(Project project) {
    if (!doesProjectExistWithId(project.getId())) {
      throw new ProjectNotFoundException();
    }
    projectRepo.save(project);
  }

  @Override
  public List<Project> find(ProjectCode projectCode) {
    return projectRepo.findProjectByProjectCode(projectCode);
  }

  @Override
  public Optional<Project> find(ProjectId projectId) {
    return projectRepo.findById(projectId);
  }

  /**
   * Updates the lastModified time of the project. Does not check credentials, as the jobrunner
   * needs to call it. 
   * <p>
   * <b>Use with care!</b>
   * @param projectId  the id of the project to update
   * @param modifiedOn the Instant object denoting the time the project was updated
   */
  @Override
  public void unsafeUpdateLastModified(ProjectId projectId, Instant modifiedOn) {
    var project = projectRepo.findById(projectId).orElseThrow(ProjectNotFoundException::new);
    project.setLastModified(modifiedOn);
    projectRepo.save(project);
  }

  private boolean doesProjectExistWithId(ProjectId id) {
    return projectRepo.findById(id).isPresent();
  }
}
