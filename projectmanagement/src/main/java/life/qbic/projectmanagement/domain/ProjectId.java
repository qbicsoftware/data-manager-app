package life.qbic.projectmanagement.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;


/**
 * The unique identifier of a project
 *
 * @param uuid
 */
public record ProjectId(UUID uuid) implements Serializable {

  @Serial
  private static final long serialVersionUID = 7904987287799381970L;

  public ProjectId {
    Objects.requireNonNull(uuid);
  }

  private ProjectId() {
    this(UUID.randomUUID());
  }

  public static ProjectId create() {
    return new ProjectId();
  }
}
