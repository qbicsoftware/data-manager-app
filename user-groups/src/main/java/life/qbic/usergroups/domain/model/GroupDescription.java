package life.qbic.usergroups.domain.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

/**
 * <b>Group description</b>
 * <p>
 * Represents an optional, free-form description of a user group.
 *
 * <p>The description is optional (may be null/empty) and limited to 500 characters.
 *
 * @since 1.19.0
 */
public class GroupDescription implements Serializable {

  private static final int MAX_LENGTH = 500;

  @Serial
  private static final long serialVersionUID = 4567890123456789012L;

  private final String value;

  /**
   * Creates a group description object instance from a String representation.
   *
   * @param s the description String; may be {@code null} or blank for "no description"
   * @return the group description
   * @throws GroupDescriptionValidationException if the description exceeds 500 characters
   * @since 1.19.0
   */
  public static GroupDescription from(String s) {
    if (s == null || s.isBlank()) {
      return new GroupDescription(null);
    }
    String trimmed = s.trim();
    if (trimmed.length() > MAX_LENGTH) {
      throw new GroupDescriptionValidationException(
          "Group description must not exceed " + MAX_LENGTH + " characters.", s);
    }
    return new GroupDescription(trimmed);
  }

  private GroupDescription(String value) {
    this.value = value;
  }

  /**
   * Queries the group description String representation.
   *
   * @return the description as String, or an empty {@link Optional} if none was set
   * @since 1.19.0
   */
  public Optional<String> value() {
    return Optional.ofNullable(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GroupDescription that = (GroupDescription) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return "GroupDescription{" +
        "value='" + value + '\'' +
        '}';
  }

  /**
   * <h1>Exception for invalid group description values</h1>
   */
  public static class GroupDescriptionValidationException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 5678901234567890123L;

    private final String value;

    public GroupDescriptionValidationException(String message, String value) {
      super(message);
      this.value = value;
    }

    public String value() {
      return value;
    }
  }
}