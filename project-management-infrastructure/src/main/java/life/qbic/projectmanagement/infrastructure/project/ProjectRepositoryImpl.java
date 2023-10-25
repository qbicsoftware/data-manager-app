package life.qbic.projectmanagement.infrastructure.project;

import static life.qbic.logging.service.LoggerFactory.logger;
import static life.qbic.projectmanagement.infrastructure.project.ProjectRepositoryImpl.ProjectRole.ADMIN;
import static life.qbic.projectmanagement.infrastructure.project.ProjectRepositoryImpl.ProjectRole.PROJECT_MANAGER;
import static org.springframework.security.acls.domain.BasePermission.ADMINISTRATION;
import static org.springframework.security.acls.domain.BasePermission.CREATE;
import static org.springframework.security.acls.domain.BasePermission.DELETE;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

import java.util.List;
import java.util.Optional;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.authorities.aspects.CanCreateProject;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
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
@Service
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
    projectRepo.save(project);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    QbicUserDetails details = (QbicUserDetails) authentication.getPrincipal();
    projectAccessService.grant(details.getUserId(), project.getId(),
        List.of(READ, WRITE));
    projectAccessService.grantToAuthority(ADMIN.auth(), project.getId(), ADMIN.permissions());
    projectAccessService.grantToAuthority(PROJECT_MANAGER.auth(), project.getId(),
        PROJECT_MANAGER.permissions());
    try {
      projectDataRepo.add(project);
    } catch (Exception e) {
      log.error("Could not add project to openBIS. Removing project from repository, as well.");
      projectRepo.delete(project);
      throw e;
    }
  }

  @Override
  @PreAuthorize("hasPermission(#project.id, 'life.qbic.controlling.domain.model.project.Project', 'WRITE')")
  public void update(Project project) {
    if (!doesProjectExistWithId(project.getId())) {
      throw new ProjectNotFoundException();
    }
    projectRepo.save(project);
  }

  @Override
  @PostFilter("hasPermission(filterObject, 'READ')")
  public List<Project> find(ProjectCode projectCode) {
    return projectRepo.findProjectByProjectCode(projectCode);
  }

  @Override
  public Optional<Project> find(ProjectId projectId) {
    return projectRepo.findById(projectId);
  }

  @Override
  public void deleteByProjectCode(ProjectCode projectCode) {
    projectDataRepo.delete(projectCode);
    List<Project> projectsByProjectCode = projectRepo.findProjectByProjectCode(projectCode);
    projectRepo.deleteAll(projectsByProjectCode);
  }

  private boolean doesProjectExistWithId(ProjectId id) {
    return projectRepo.findById(id).isPresent();
  }

  public enum ProjectRole {
    ADMIN("ROLE_ADMIN", List.of(READ, WRITE, CREATE,
        DELETE, ADMINISTRATION)),
    PROJECT_MANAGER("ROLE_PROJECT_MANAGER",
        List.of(WRITE, CREATE,
            DELETE));

    private final String roleName;
    private final List<Permission> allowedPermissions;

    public GrantedAuthority auth() {
      return new SimpleGrantedAuthority(roleName);
    }

    public List<Permission> permissions() {
      return allowedPermissions;
    }

    ProjectRole(String roleName,
        List<Permission> allowedPermissions) {
      this.roleName = roleName;
      this.allowedPermissions = allowedPermissions;
    }
  }
}
