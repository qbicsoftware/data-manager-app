package life.qbic.domain.concepts.communication;

import java.io.Serial;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class NotificationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 7816744418299591709L;

  public NotificationException() {
    super();
  }

  public NotificationException(String message) {
    super(message);
  }

  public NotificationException(String message, Throwable cause) {
    super(message, cause);
  }

}
