package life.qbic.projectmanagement.domain.model.project.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * <b>Project Registered Event</b>
 * <p>
 * Domain event that indicates a new project registration.
 *
 * @since 1.0.0
 */
public class ProjectRegisteredEvent extends DomainEvent {

  @Serial
  private static final long serialVersionUID = -8611090109019335947L;
  private final String createdProject;

  private ProjectRegisteredEvent(ProjectId projectId) {
    this.createdProject = Objects.requireNonNull(projectId.value());
  }

  public static ProjectRegisteredEvent create(ProjectId projectId) {
    return new ProjectRegisteredEvent(projectId);
  }

  @JsonGetter("createdProject")
  public String createdProject() {
    return createdProject;
  }
}
