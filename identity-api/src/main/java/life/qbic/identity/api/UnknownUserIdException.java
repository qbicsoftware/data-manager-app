package life.qbic.identity.api;

import java.io.Serial;

/**
 * <b>Unknown User Id Exception</b>
 *
 * <p>Is thrown by services if a user id is not known to the system and therefore
 * a request cannot be processed successfully.</p>
 *
 * @since 1.0.0
 */
public class UnknownUserIdException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1153158396587605675L;

  public UnknownUserIdException() {
  }

  public UnknownUserIdException(String message) {
    super(message);
  }

  public UnknownUserIdException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnknownUserIdException(Throwable cause) {
    super(cause);
  }

  public UnknownUserIdException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
