package life.qbic.projectmanagement.application;

import java.util.Objects;
import life.qbic.projectmanagement.domain.Project;
import life.qbic.projectmanagement.domain.ProjectIntent;
import life.qbic.projectmanagement.domain.ProjectRepository;
import life.qbic.projectmanagement.domain.ProjectTitle;

/**
 * Application service facilitating the creation of projects.
 */
public class ProjectCreationServiceImpl implements ProjectCreationService {

  private final ProjectRepository projectRepository;

  public ProjectCreationServiceImpl(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  @Override
  public ProjectCreationResponse createProject(String title) {
    if (Objects.isNull(title)) {
      return ProjectCreationResponse.failureResponse(
          new IllegalArgumentException("Project title must not be null"));
    }
    try {
      Project project = Project.create(new ProjectIntent(new ProjectTitle(title)));
      projectRepository.add(project);
      return ProjectCreationResponse.successResponse(new ProjectCreatedEvent(project.getId()));
    } catch (RuntimeException e) {
      return ProjectCreationResponse.failureResponse(e);
    }
  }
}
