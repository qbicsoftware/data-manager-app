package life.qbic.identity.application.communication.broadcasting;

import java.io.Serial;

/**
 * <b>Subscriber</b>
 * <p>
 * A subscriber is interested in one specific event type and can be used to receive information
 * about any event that matches the type.
 *
 * @since 1.0.0
 */
public interface Subscriber {

  /**
   * Returns the type of events the subscriber is interested in.
   * <p>
   * The recommended format is <strong>camelCase</strong>, so for example a type user registered
   * should be
   * <code>"userRegistered"</code>
   *
   * @return the interested event type
   * @since 1.0.0
   */
  String type();

  /**
   * Callback method for the client, passes an integration event.
   * <p>
   * Implementation must throw a {@link WrongTypeException} if the passed integration event type
   * does not match the subscription.
   *
   * @param event the integration event to receive
   * @throws WrongTypeException if the events type returned by {@link IntegrationEvent#type()} does
   *                            not match the subscriber's type indicated with
   *                            {@link Subscriber#type()}
   * @since 1.0.0
   */
  void onReceive(IntegrationEvent event) throws WrongTypeException;

  /**
   * Exception that indicates, that the subscriber received an integration event of the wrong
   * type
   *
   * @since 1.0.0
   */
  class WrongTypeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 2025163227157683574L;

    public WrongTypeException() {
      super();
    }

    public WrongTypeException(String message) {
      super(message);
    }

    public WrongTypeException(String message, Throwable cause) {
      super(message, cause);
    }

    public WrongTypeException(Throwable cause) {
      super(cause);
    }

    public WrongTypeException(String message, Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
    }
  }

}


