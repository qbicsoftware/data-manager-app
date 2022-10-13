package life.qbic.projectmanagement.domain.project;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;


/**
 * The unique identifier of a project
 */
@Embeddable
public final class ProjectId implements Serializable {

  @Serial
  private static final long serialVersionUID = 7904987287799381970L;

  @Column(name = "uuid")
  private final String value;

  protected ProjectId() {
    this(UUID.randomUUID());
  }

  private ProjectId(UUID uuid) {
    if (Objects.isNull(uuid)) {
      throw new IllegalArgumentException("uuid must be provided");
    }
    Objects.requireNonNull(uuid);
    this.value = uuid.toString();
  }

  public static ProjectId create() {
    return new ProjectId();
  }

  public static ProjectId of(UUID uuid) {
    return new ProjectId(uuid);
  }

  public static ProjectId parse(String str) throws IllegalArgumentException {
    UUID id = UUID.fromString(str);
    return new ProjectId(id);
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ProjectId.class.getSimpleName() + "[", "]")
        .add("uuid=" + value)
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

    return value.equals(projectId.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
