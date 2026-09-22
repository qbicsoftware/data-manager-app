package life.qbic.usergroups.domain.model;

import jakarta.persistence.Column;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * <b>Group Id</b>
 * <p>
 * A unique identifier of a user group in the user groups context.
 *
 * <p>Mirrors {@code life.qbic.identity.domain.model.UserId}: UUID-backed, usable as an
 * {@code @EmbeddedId}.
 *
 * @since 1.19.0
 */
public class GroupId implements Serializable {

  @Serial
  private static final long serialVersionUID = 1034567890123456701L;

  @Column(name = "id")
  private String value;

  protected GroupId() {
    super();
  }

  /**
   * Creates a new random group id.
   *
   * @return a group id
   * @since 1.19.0
   */
  public static GroupId create() {
    return new GroupId(UUID.randomUUID());
  }

  /**
   * Creates a group id from a String based representation.
   * <p>
   * Throws an {@link IllegalArgumentException} if the input is not a valid group id.
   *
   * @param s a group id String
   * @return an instance of a new group id object
   * @since 1.19.0
   */
  public static GroupId from(String s) throws IllegalArgumentException {
    try {
      return new GroupId(UUID.fromString(s));
    } catch (IllegalArgumentException ignored) {
      throw new IllegalArgumentException(s + " has unknown group id format.");
    }
  }

  private GroupId(UUID id) {
    super();
    this.value = id.toString();
  }

  /**
   * Queries the String value of the group id.
   *
   * @return the group id value
   * @since 1.19.0
   */
  public String get() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupId groupId = (GroupId) o;
    return Objects.equals(value, groupId.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "GroupId{" +
        "value=" + value +
        '}';
  }
}