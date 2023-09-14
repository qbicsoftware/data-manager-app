package life.qbic.domain.concepts.communication;

import java.io.Serial;

/**
 * <b>Communication Exception</b>
 *
 * <p>Exception that shall be thrown to indicate issues during communication with the user</p>
 *
 * @since 1.0.0
 */
public class CommunicationException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 7816744418299591709L;

  public CommunicationException() {
    super();
  }

  public CommunicationException(String message) {
    super(message);
  }

  public CommunicationException(String message, Throwable cause) {
    super(message, cause);
  }

}
