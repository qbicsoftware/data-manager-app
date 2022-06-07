package life.qbic.domain.user;

import java.util.Objects;

/**
 * <b>Full name</b>
 * <p>
 * Represents a full name of a user.
 *
 * @since 1.0.0
 */
public class FullName {

  private String name;

  /**
   * Creates a full name object instance from a String representation.
   * <p>
   * Performs some basic input validation, for example the input String must not be empty or blank.
   *
   * @param s the full name String representation
   * @return the full name
   * @since 1.0.0
   */
  public static FullName from(String s) {
    if (s.isBlank()) {
      throw new InvalidFullNameException("Name must not be empty or blank.");
    }
    var fullName = new FullName();
    fullName.setName(s);
    return fullName;
  }

  private FullName() {
    super();
  }

  private void setName(String s) {
    this.name = s;
  }

  /**
   * Queries the full name String representation.
   *
   * @return the full name as String
   * @since 1.0.0
   */
  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FullName fullName1 = (FullName) o;
    return Objects.equals(name, fullName1.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public static class InvalidFullNameException extends RuntimeException {

    InvalidFullNameException() {
      super();
    }

    InvalidFullNameException(String message) {
      super(message);
    }
  }
}
