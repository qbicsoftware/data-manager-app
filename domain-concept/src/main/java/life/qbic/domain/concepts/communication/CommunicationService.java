package life.qbic.domain.concepts.communication;

/**
 * <b>Notification service</b>
 *
 * <p>Provides methods to send out notifications to recipients</p>
 *
 * @since 1.0.0
 */
public interface CommunicationService {

  /**
   * Sends a message (e.g. email) to a recipient
   * @param subject The subject of the message
   * @param recipient The Recipient of the message
   * @param content The Content of the message
   * @throws CommunicationException
   */
  void send(Subject subject, Recipient recipient, Content content) throws CommunicationException;

  /**
   * Sends a message (e.g. email) with an attached file to a recipient
   * @param subject The subject of the message
   * @param recipient The Recipient of the message
   * @param content The Content of the message
   * @param attachment An Attachment object denoting name and content of the attached file
   * @throws CommunicationException
   */
  void send(Subject subject, Recipient recipient, Content content, Attachment attachment) throws CommunicationException;
}
