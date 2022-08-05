package life.qbic.logging.subscription.provider.email;

import java.io.IOException;
import java.util.Properties;
import life.qbic.logging.subscription.provider.email.property.EmailPropertyLoader;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface EmailService {

    static EmailService instance() {
      Properties props;
      try {
        props = EmailPropertyLoader.create().load();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return new SimpleEmailService(props);
    }

    void send(String subject, String message, String sender, String recipient);


}
