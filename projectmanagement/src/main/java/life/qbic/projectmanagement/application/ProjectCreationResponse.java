package life.qbic.projectmanagement.application;

import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationResponse;

/**
 * A response for project creations.
 */
public class ProjectCreationResponse extends ApplicationResponse {

  private String projectId;


  public static ProjectCreationResponse successResponse(String projectId) {
    var successResponse = new ProjectCreationResponse();
    successResponse.setType(Type.SUCCESSFUL);
    successResponse.setProjectId(projectId);
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

  protected void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public Optional<String> createdProject() {
    return Optional.ofNullable(projectId);
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

    return Objects.equals(projectId, that.projectId);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (projectId != null ? projectId.hashCode() : 0);
    return result;
  }
}
