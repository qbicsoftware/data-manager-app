package life.qbic.identityaccess.application.notification;

import java.time.Instant;

/**
 * <b>Message Parameters</b>
 *
 * <p>Provides quick access to important message properties, such as the message type, its id and
 * when it the event it reports has happened.
 *
 * @since 1.0.0
 */
public final class MessageParameters {

  public final String messageType;

  public final String messageId;

  public final String occurredOn;

  private MessageParameters(String messageType, String messageId, String occurredOn) {
    super();
    this.messageId = messageId;
    this.messageType = messageType;
    this.occurredOn = occurredOn;
  }

  public static MessageParameters durableTextParameters(
      String messageType, String messageId, Instant occurredOn) {
    String occurredOnString = occurredOn.toString();
    return new MessageParameters(messageType, messageId, occurredOnString);
  }
}
