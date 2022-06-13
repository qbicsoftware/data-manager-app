package life.qbic.domain.usermanagement.registration;

/**
 * <b>User Not Found exception</b>
 * <p>
 * Throw this exception, if you want to indicate to the client that a user could not been found in
 * the system.
 *
 * @since 1.0.0
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
