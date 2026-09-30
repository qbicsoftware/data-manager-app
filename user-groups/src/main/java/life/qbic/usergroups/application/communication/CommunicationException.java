package life.qbic.usergroups.application.communication;

/**
 * <b>Communication exception</b>
 *
 * <p>Thrown when an email initiated by the user groups context could not be sent. The exception
 * is deliberately unchecked in the style of the other contexts' communication exceptions, so the
 * notification policy can surface a failure without leaking infrastructure details to callers.</p>
 *
 * @since 1.20.0
 */
public class CommunicationException extends RuntimeException {

  private static final long serialVersionUID = -6012222222222222222L;

  public CommunicationException(String message) {
    super(message);
  }

  public CommunicationException(String message, Throwable cause) {
    super(message, cause);
  }
}