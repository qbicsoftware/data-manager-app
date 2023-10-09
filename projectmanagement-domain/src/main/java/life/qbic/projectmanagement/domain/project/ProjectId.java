package life.qbic.projectmanagement.domain.project;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;


/**
 * The unique identifier of a project
 */
@Embeddable
public class ProjectId implements Serializable {

  @Serial
  private static final long serialVersionUID = 7904987287799381970L;

  @Column(name = "projectId")
  private final String projectId;

  protected ProjectId() {
    this(UUID.randomUUID());
  }

  private ProjectId(UUID uuid) {
    if (Objects.isNull(uuid)) {
      throw new IllegalArgumentException("uuid must be provided");
    }
    Objects.requireNonNull(uuid);
    this.projectId = uuid.toString();
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

  public static boolean isValid(String projectID) {
    try {
      UUID.fromString(projectID);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public String value() {
    return projectId;
  }

  @Override
  public String toString() {
    return projectId; //IMPORTANT: This method is used by spring security {@link org.springframework.security.acls.jdbc.BasicLookupStrategy.class} to compare ACL entries.
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    // a project ID equals the string of its uuid
    if (o.getClass().equals(String.class)) {
      return this.projectId.equals(o);
    }
    if (getClass() != o.getClass()) {
      return false;
    }

    ProjectId projectId = (ProjectId) o;

    return this.projectId.equals(projectId.projectId);
  }

  @Override
  public int hashCode() {
    return projectId.hashCode();
  }
}
