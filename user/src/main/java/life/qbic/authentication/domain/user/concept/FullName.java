package life.qbic.authentication.domain.user.concept;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;

/**
 * <b>Full name</b>
 * <p>
 * Represents a full name of a user.
 *
 * @since 1.0.0
 */
public class FullName implements Serializable {

  private final String value;

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
      throw new FullNameValidationException("Name must not be empty or blank.", s);
    }
    return new FullName(s);
  }

  private FullName(String fullName) {
    super();
    this.value = fullName;
  }

  /**
   * Queries the full name String representation.
   *
   * @return the full name as String
   * @since 1.0.0
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
    FullName fullName1 = (FullName) o;
    return Objects.equals(value, fullName1.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  /**
   * <h1>Exception that indicates violations during the full name validation process/h1>
   *
   * <p>This exception is supposed to be thrown, if the provided full name for an user consists of
   * only whitespace or is Empty. Its intention is to contain the invalid full name</p>
   *
   * @since 1.0.0
   */

  public static class FullNameValidationException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -7328021953844399851L;

    private final String invalidFullName;

    FullNameValidationException(String message, String invalidFullName) {
      super(message);
      this.invalidFullName = invalidFullName;
    }

    public String getInvalidFullName() {
      return invalidFullName;
    }
  }
}
