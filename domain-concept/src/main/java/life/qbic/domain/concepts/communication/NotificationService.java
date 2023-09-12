package life.qbic.domain.concepts.communication;

import javax.security.auth.Subject;
import javax.swing.text.AbstractDocument.Content;

/**
 * <b>Notification service</b>
 *
 * <p>Provides methods to send out notifications to recipients</p>
 *
 * @since 1.0.0
 */
public interface NotificationService {

  void send(Subject subject, Recipient recipient, Content content);

}
