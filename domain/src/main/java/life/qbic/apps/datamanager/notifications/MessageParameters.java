package life.qbic.apps.datamanager.notifications;

import java.time.Instant;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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

  public static MessageParameters durableTextParameters(String messageType, String messageId,
      Instant occuredOn) {
    String occuredOnString = occuredOn.toString();
    return new MessageParameters(messageType, messageId, occuredOnString);
  }

}
