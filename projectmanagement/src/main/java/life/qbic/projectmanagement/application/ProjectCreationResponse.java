package life.qbic.projectmanagement.application;

import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationResponse;

/**
 * A response for project creations.
 */
public class ProjectCreationResponse extends ApplicationResponse {

  private String projectTitle;


  public static ProjectCreationResponse successResponse(String projectTitle) {
    var successResponse = new ProjectCreationResponse();
    successResponse.setType(Type.SUCCESSFUL);
    successResponse.setProjectTitle(projectTitle);
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

  protected void setProjectTitle(String projectId) {
    this.projectTitle = projectId;
  }

  public Optional<String> createdProject() {
    return Optional.ofNullable(projectTitle);
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

    return Objects.equals(projectTitle, that.projectTitle);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (projectTitle != null ? projectTitle.hashCode() : 0);
    return result;
  }
}
