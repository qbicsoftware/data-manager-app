package life.qbic.projectmanagement.application.pinned;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key of a {@link PinnedProject}: the owning user and the project the user
 * pinned.
 * <p>
 * The project is referenced by its plain identifier string instead of by an embedded
 * {@code ProjectId}, because a pin is not project state and must survive the loss of read access to
 * the referenced project (see ADR-0008).
 *
 * @since 1.12.0
 */
@Embeddable
public class PinnedProjectId implements Serializable {

  @Serial
  private static final long serialVersionUID = 1163735371417640913L;

  @Column(name = "userId", nullable = false, updatable = false)
  private String userId;

  @Column(name = "projectId", nullable = false, updatable = false)
  private String projectId;

  protected PinnedProjectId() {
    // required by JPA
  }

  private PinnedProjectId(String userId, String projectId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    if (projectId == null || projectId.isBlank()) {
      throw new IllegalArgumentException("projectId must not be blank");
    }
    this.userId = userId;
    this.projectId = projectId;
  }

  static PinnedProjectId of(String userId, String projectId) {
    return new PinnedProjectId(userId, projectId);
  }

  public String userId() {
    return userId;
  }

  public String projectId() {
    return projectId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PinnedProjectId that = (PinnedProjectId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(projectId, that.projectId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, projectId);
  }

  @Override
  public String toString() {
    return "PinnedProjectId{userId=" + userId + ", projectId=" + projectId + '}';
  }
}
