package life.qbic.projectmanagement.application;

import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.projectmanagement.domain.Project;

/**
 * A response for project creations.
 */
public class ProjectCreationResponse extends ApplicationResponse {

  private Project createdProject;


  public static ProjectCreationResponse successResponse(Project project) {
    var successResponse = new ProjectCreationResponse();
    successResponse.setType(Type.SUCCESSFUL);
    successResponse.setCreatedProject(project);
    return successResponse;
  }

  public static ProjectCreationResponse failureResponse(RuntimeException... exceptions) {
    if (exceptions == null) {
      throw new IllegalArgumentException("Null references are not allowed.");
    }
    var failureResponse = new ProjectCreationResponse();
    failureResponse.setType(Type.FAILED);
    failureResponse.setExceptions(exceptions);
    return failureResponse;
  }

  protected void setCreatedProject(Project createdProject) {
    this.createdProject = createdProject;
  }

  public Optional<Project> createdProject() {
    return Optional.ofNullable(createdProject);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    ProjectCreationResponse that = (ProjectCreationResponse) o;

    return Objects.equals(createdProject, that.createdProject);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (createdProject != null ? createdProject.hashCode() : 0);
    return result;
  }
}
