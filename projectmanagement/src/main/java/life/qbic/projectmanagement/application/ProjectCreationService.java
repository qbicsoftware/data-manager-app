package life.qbic.projectmanagement.application;

import static life.qbic.logging.service.LoggerFactory.logger;

import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.*;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectIntent;
import life.qbic.projectmanagement.domain.project.ProjectTitle;
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
    try {
      projectTitle = ProjectTitle.create(title);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException(ErrorCode.INVALID_PROJECT_TITLE,
          ErrorParameters.of(200, title)));
    }

    ProjectObjective projectObjective;
    try {
      projectObjective = ProjectObjective.create(objective);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException(ErrorCode.INVALID_PROJECT_OBJECTIVE,
          ErrorParameters.create()));
    }

    try {
      ProjectIntent intent = new ProjectIntent(projectTitle, projectObjective);
      Project project = Project.create(intent);
      projectRepository.add(project);
      return Result.success(project);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException());
    }
  }

  public Result<Project, ApplicationException> createProjectWithExperimentalDesign(String title,
      String objective,
      String experimentalDesign) {

    ProjectTitle projectTitle;
    try {
      projectTitle = ProjectTitle.create(title);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException(ErrorCode.INVALID_PROJECT_TITLE,
          ErrorParameters.of(title)));
    }

    ProjectObjective projectObjective;
    try {
      projectObjective = ProjectObjective.create(objective);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException(ErrorCode.INVALID_PROJECT_OBJECTIVE,
          ErrorParameters.create()));
    }

    ExperimentalDesignDescription experimentalDesignDescription;
    try {
      experimentalDesignDescription = ExperimentalDesignDescription.create(experimentalDesign);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException(ErrorCode.INVALID_EXPERIMENTAL_DESIGN,
          ErrorParameters.of(ExperimentalDesignDescription.maxLength(), experimentalDesign)));
    }

    try {
      ProjectIntent intent = new ProjectIntent(projectTitle, projectObjective).with(
          experimentalDesignDescription);
      Project project = Project.create(intent);
      projectRepository.add(project);
      return Result.success(project);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      return Result.failure(new ProjectManagementException());
    }
  }
}
