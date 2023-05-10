package life.qbic.projectmanagement.domain.project.event;

import com.fasterxml.jackson.annotation.JsonGetter;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectRegisteredEvent extends DomainEvent {

  @Serial
  private static final long serialVersionUID = -8611090109019335947L;
  private final Instant occurredOn;

  private final String createdProject;

  private ProjectRegisteredEvent(Instant occurredOn, ProjectId projectId) {
    this.occurredOn = Objects.requireNonNull(occurredOn);
    this.createdProject = Objects.requireNonNull(projectId.value());
  }

  public static ProjectRegisteredEvent create(ProjectId projectId) {
    return new ProjectRegisteredEvent(Instant.now(), projectId);
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }

  @JsonGetter("createdProject")
  public String createdProject() {
    return createdProject;
  }
}
