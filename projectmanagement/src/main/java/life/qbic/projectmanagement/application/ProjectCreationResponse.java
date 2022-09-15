package life.qbic.projectmanagement.application;

import java.util.Optional;
import life.qbic.application.commons.ApplicationResponse;

/**
 * A response for project creations.
 */
public class ProjectCreationResponse extends ApplicationResponse {

  private ProjectCreatedEvent projectCreatedEvent;


  public static ProjectCreationResponse successResponse(ProjectCreatedEvent projectCreatedEvent) {
    var successResponse = new ProjectCreationResponse();
    successResponse.setType(Type.SUCCESSFUL);
    successResponse.setProjectCreatedEvent(projectCreatedEvent);
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

  private void setProjectCreatedEvent(ProjectCreatedEvent projectCreatedEvent) {
    this.projectCreatedEvent = projectCreatedEvent;
  }

  public Optional<ProjectCreatedEvent> projectCreatedEvent() {
    return Optional.ofNullable(projectCreatedEvent);
  }
}
