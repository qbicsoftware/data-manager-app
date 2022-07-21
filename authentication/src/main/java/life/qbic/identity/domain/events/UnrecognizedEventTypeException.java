package life.qbic.identity.domain.events;

/**
 * Thrown when an event type is not recognized. This should never happen.
 *
 * @since 1.0.0
 */
public class UnrecognizedEventTypeException extends RuntimeException {

  public UnrecognizedEventTypeException(String message) {
    super(message);
  }
}
