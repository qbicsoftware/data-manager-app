package life.qbic.apps.datamanager.notifications;

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

  public final String occuredOn;

  private MessageParameters(String messageType, String messageId, String occuredOn) {
    super();
    this.messageId = messageId;
    this.messageType = messageType;
    this.occuredOn = occuredOn;
  }

  public static MessageParameters durableTextParameters(
      String messageType, String messageId, Instant occuredOn) {
    String occuredOnString = occuredOn.toString();
    return new MessageParameters(messageType, messageId, occuredOnString);
  }
}
