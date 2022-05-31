package life.qbic.domain.usermanagement.registration;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException() {
    super();
  }

  public UserNotFoundException(String message) {
    super(message);
  }

  public UserNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
