package life.qbic.logging.subscription.provider.mail;

import java.io.IOException;
import java.util.Properties;
import life.qbic.logging.subscription.provider.mail.property.MailPropertyLoader;

/**
 * <b>Mail Service</b>
 * <p>
 * Mail service interface to send mails.
 *
 * @since 1.0.0
 */
public interface MailService {

  static MailService instance() {
    Properties props;
    try {
      props = MailPropertyLoader.create().load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return EMailService.create(props);
  }

  void send(String subject, String message, String sender, String recipient);


}
