package life.qbic.domain.concepts.communication;

/**
 * <b>Notification service</b>
 *
 * <p>Provides methods to send out notifications to recipients</p>
 *
 * @since 1.0.0
 */
public interface CommunicationService {

  void send(Subject subject, Recipient recipient, Content content) throws CommunicationException;

}
