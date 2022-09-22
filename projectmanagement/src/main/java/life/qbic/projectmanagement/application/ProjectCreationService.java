package life.qbic.projectmanagement.application;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.*;
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository;

/**
 * Application service facilitating the creation of projects.
 */
public class ProjectCreationService {

  private static final Logger log = logger(ProjectCreationService.class);

  private final ProjectRepository projectRepository;

  public ProjectCreationService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  /**
   * Create a new project based on the information provided.
   *
   * @param title the title of the project.
   * @return the created project
   */
  public Result<Project, ApplicationException> createProject(String title, String objective) {
    ProjectTitle projectTitle;
    ProjectObjective projectObjective;
    try {
      projectTitle = new ProjectTitle(title);
      projectObjective = new ProjectObjective(objective);
    } catch (ProjectManagementDomainException e) {
      return Result.failure(
          new ProjectManagementException("Could not create project title for: " + title, e,
              ErrorCode.INVALID_PROJECT_TITLE,
              ErrorParameters.create().with("projectTitle", title)));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException());
    }
    Project project;
    try {
      project = Project.create(new ProjectIntent(projectTitle,projectObjective));
    } catch (Exception e) {
      log.error("Could not create project with title " + projectTitle, e);
      return Result.failure(new ProjectManagementException());
    }
    try {
      projectRepository.add(project);
    } catch (Exception e) {
      log.error("Could not add project " + project, e);
      return Result.failure(new ProjectManagementException());
    }
    return Result.success(project);
  }
}
