package life.qbic.projectmanagement.domain;

import java.util.Objects;
import java.util.UUID;


/**
 * The unique identifier of a project
 *
 * @param uuid
 */
public record ProjectId(UUID uuid) {

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
