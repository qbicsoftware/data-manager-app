package life.qbic.projectmanagement.application;

import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.domain.Project;
import life.qbic.projectmanagement.domain.ProjectIntent;
import life.qbic.projectmanagement.domain.ProjectRepository;
import life.qbic.projectmanagement.domain.ProjectTitle;

/**
 * Application service facilitating the creation of projects.
 */
public class ProjectCreationService {

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
  public Result<Project, ApplicationException> createProject(String title) {
    ProjectTitle projectTitle;
    try {
      projectTitle = new ProjectTitle(title);
    } catch (Exception e) {
      return Result.failure(
          new ProjectManagementException("Could not create project title for: " + title, e,
              ErrorCode.INVALID_PROJECT_TITLE,
              ErrorParameters.create().with("projectTitle", title)));
    }
    Project project;
    try {
      project = Project.create(new ProjectIntent(projectTitle));
    } catch (Exception e) {
      return Result.failure(
          new ProjectManagementException(
              "Could not create project intent with title: " + projectTitle, e));
    }
    try {
      projectRepository.add(project);
    } catch (Exception e) {
      return Result.failure(new ProjectManagementException("Could not add project: " + project, e));
    }
    return Result.success(project);
  }
}
