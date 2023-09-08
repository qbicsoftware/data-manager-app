package life.qbic.projectmanagement.domain.project.service.event;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectAccessGranted extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 199678646014632540L;
  private final String userId;
  private final String projectId;

  private final String projectTitle;

  private final Instant occurredOn;

  private ProjectAccessGranted(Instant occurredOn, String userId, String projectId, String projectTitle) {
    this.userId = Objects.requireNonNull(userId);
    this.projectId = Objects.requireNonNull(projectId);
    this.projectTitle = Objects.requireNonNull(projectTitle);
    this.occurredOn = occurredOn;
  }

  public static ProjectAccessGranted create(String userId, String projectId, String projectTitle) {
    return new ProjectAccessGranted(Instant.now(), userId, projectId, projectTitle);
  }

  @Override
  public Instant occurredOn() {
    return this.occurredOn;
  }

  public String forUser() {
    return userId;
  }

  public String forProject() {
    return projectId;
  }

  public String withTitle() {
    return projectTitle; }
}
