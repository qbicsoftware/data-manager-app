package life.qbic.usergroups.domain.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * <b>Group name</b>
 * <p>
 * Represents the (display) name of a user group.
 *
 * <p>Validates non-blank input and a maximum length of 80 characters. Uniqueness is deliberately
 * NOT part of this value object — uniqueness is a cross-aggregate invariant enforced by the
 * repository/application layer against the case-insensitive database collation
 * ({@code utf8mb4_unicode_ci}).
 *
 * @since 1.19.0
 */
public class GroupName implements Serializable {

  private static final int MAX_LENGTH = 80;

  @Serial
  private static final long serialVersionUID = 2345678901234567801L;

  private final String value;

  /**
   * Creates a group name object instance from a String representation.
   *
   * @param s the group name String representation
   * @return the group name
   * @throws GroupNameValidationException if the name is blank or longer than 80 characters
   * @since 1.19.0
   */
  public static GroupName from(String s) {
    if (s != null) {
      s = s.trim();
    }
    if (s == null || s.isBlank()) {
      throw new GroupNameValidationException("Group name must not be empty or blank.", s);
    }
    if (s.length() > MAX_LENGTH) {
      throw new GroupNameValidationException(
          "Group name must not exceed " + MAX_LENGTH + " characters.", s);
    }
    return new GroupName(s);
  }

  private GroupName(String name) {
    this.value = name;
  }

  /**
   * Queries the group name String representation.
   *
   * @return the group name as String
   * @since 1.19.0
   */
  public String value() {
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
    GroupName groupName = (GroupName) o;
    return Objects.equals(value, groupName.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "GroupName{" +
        "value='" + value + '\'' +
        '}';
  }

  /**
   * <h1>Exception for invalid group name values</h1>
   */
  public static class GroupNameValidationException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 3456789012345678901L;

    private final String value;

    public GroupNameValidationException(String message, String value) {
      super(message);
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
}