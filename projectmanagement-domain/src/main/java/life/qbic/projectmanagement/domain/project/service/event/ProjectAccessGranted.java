package life.qbic.projectmanagement.domain.project.service.event;

import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;

/**
 * <b>Project Access Granted Event</b>
 *
 * <p>This event is emitted after access has been granted to a user</p>
 *
 * @since 1.0.0
 */
public class ProjectAccessGranted extends DomainEvent {

  @Serial
  private static final long serialVersionUID = 199678646014632540L;
  private final String userId;
  private final String projectId;

  private final String projectTitle;

  private final Instant occurredOn;

  private ProjectAccessGranted(Instant occurredOn, String userId, String projectId,
      String projectTitle) {
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

  public String forUserId() {
    return userId;
  }

  public String forProjectId() {
    return projectId;
  }

  public String forProjectTitle() {
    return projectTitle;
  }
}
