package life.qbic.projectmanagement.domain.service.event;

import java.io.Serial;
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
  private static final long serialVersionUID = -3950161360758950786L;
  private final String userId;
  private final String projectId;

  private ProjectAccessGranted(String userId, String projectId) {
    this.userId = Objects.requireNonNull(userId);
    this.projectId = Objects.requireNonNull(projectId);
  }

  public static ProjectAccessGranted create(String userId, String projectId) {
    return new ProjectAccessGranted(userId, projectId);
  }

  public String forUserId() {
    return userId;
  }

  public String forProjectId() {
    return projectId;
  }
}
