package life.qbic.projectmanagement.persistence;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Optional;
import life.qbic.authorization.ProjectAccessService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.Authentication;
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
  @PreAuthorize("hasAuthority('project:create')")
  @Transactional
  public void add(Project project) {
    ProjectCode projectCode = project.getProjectCode();
    if (doesProjectExistWithId(project.getId()) || projectDataRepo.projectExists(projectCode)) {
      throw new ProjectExistsException();
    }
    projectRepo.save(project);
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    projectAccessService.grant(authentication.getName(), project.getId(), BasePermission.READ);
    projectAccessService.grant(authentication.getName(), project.getId(), BasePermission.WRITE);
    try {
      projectDataRepo.add(project.getProjectCode());
    } catch (Exception e) {
      log.error("Could not add project to openBIS. Removing project from repository, as well.");
      projectRepo.delete(project);
      throw e;
    }
  }

  @Override
  @PreAuthorize("hasPermission(#project.id, 'life.qbic.projectmanagement.domain.project.Project', 'WRITE')")
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
}
