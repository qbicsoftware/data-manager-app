package life.qbic.projectmanagement.infrastructure.project;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.time.Instant;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectRole;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


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
  private final AuthenticationToUserIdTranslationService userIdTranslator;

  @Autowired
  public ProjectRepositoryImpl(QbicProjectRepo projectRepo,
      QbicProjectDataRepo projectDataRepo, ProjectAccessService projectAccessService,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.projectRepo = projectRepo;
    this.projectDataRepo = projectDataRepo;
    this.projectAccessService = projectAccessService;
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
  }

  @Override
  public void add(Project project) {
    ProjectCode projectCode = project.getProjectCode();
    if (doesProjectExistWithId(project.getId()) || projectDataRepo.projectExists(projectCode)) {
      throw new ProjectExistsException();
    }
    try {
      var savedProject = projectRepo.save(project);
      var userId = userIdTranslator.translateToUserId(
              SecurityContextHolder.getContext().getAuthentication())
          .orElseThrow();
      projectAccessService.initializeProject(savedProject.getId(), userId);
      projectAccessService.addAuthorityAccess(savedProject.getId(),
          "ROLE_ADMIN", ProjectAccessService.ProjectRole.ADMIN);
      projectAccessService.addAuthorityAccess(savedProject.getId(), "ROLE_PROJECT_MANAGER",
          ProjectRole.ADMIN);
      projectDataRepo.add(project);
    } catch (Exception e) {
      log.error("An exception occurred while adding a new project: " + project.getProjectCode());
      log.error("Project title was: " + project.getProjectIntent().projectTitle());
      projectRepo.delete(project);
      projectDataRepo.delete(project.getProjectCode());
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
  public boolean existsProjectByProjectCode(ProjectCode projectCode) {
    return projectRepo.existsProjectByProjectCode(projectCode);
  }

  @Override
  public Optional<Project> find(ProjectId projectId) {
    return projectRepo.findById(projectId);
  }

  /**
   * Saves a project to the repository.
   * @param project the project to save persistently
   * @since 1.11.1
   */
  public void save(Project project) {
    projectRepo.save(project);
  }

  private boolean doesProjectExistWithId(ProjectId id) {
    return projectRepo.findById(id).isPresent();
  }
}
