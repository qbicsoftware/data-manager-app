package life.qbic.projectmanagement.application;

import java.util.Objects;
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
  public Project createProject(String title) {
    if (Objects.isNull(title)) {
      throw new ProjectManagementException("Project title must not be null. Provided: " + title,
          new IllegalArgumentException("Project title must not be null"));
    }

    Project project;
    try {
      project = Project.create(new ProjectIntent(new ProjectTitle(title)));
    } catch (Exception e) {
      throw new ProjectManagementException("Could not create project with title: " + title, e);
    }
    try {
      projectRepository.add(project);
    } catch (Exception e) {
      throw new ProjectManagementException("Could not add project: " + project, e);
    }
    return project;
  }
}
