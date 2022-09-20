package life.qbic.projectmanagement.domain.project;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;


/**
 * The unique identifier of a project
 */
public final class ProjectId implements Serializable {

  @Serial
  private static final long serialVersionUID = 7904987287799381970L;

  private UUID uuid;

  private ProjectId() {
    this(UUID.randomUUID());
  }

  private ProjectId(UUID uuid) {
    if (Objects.isNull(uuid)) {
      throw new IllegalArgumentException("uuid must be provided");
    }
    Objects.requireNonNull(uuid);
    this.uuid = uuid;
  }

  public static ProjectId create() {
    return new ProjectId();
  }

  public static ProjectId of(UUID uuid) {
    return new ProjectId(uuid);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ProjectId.class.getSimpleName() + "[", "]")
        .add("uuid=" + uuid)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProjectId projectId = (ProjectId) o;

    return uuid.equals(projectId.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
