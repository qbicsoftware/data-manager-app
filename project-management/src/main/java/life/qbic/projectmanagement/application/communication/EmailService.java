package life.qbic.projectmanagement.application.communication;

/**
 * <b>Email Service</b>
 *
 * <p>Decouples the implementation detail from the functionality that
 * is required by the application layer.</p>
 * <p>
 * Offers an interface to send emails to recipients.
 *
 * @since 1.0.0
 */
public interface EmailService {


  void send(
      Subject subject, Recipient recipient, Content content) throws CommunicationException;

  void send(
      Subject subject, Recipient recipient, Content content, Attachment attachment) throws CommunicationException;

}
