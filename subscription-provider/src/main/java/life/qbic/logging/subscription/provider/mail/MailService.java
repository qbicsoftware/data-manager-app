package life.qbic.logging.subscription.provider.mail;

import life.qbic.logging.subscription.provider.mail.property.MailProperties;
import org.springframework.stereotype.Component;

/**
 * <b>Mail Service</b>
 * <p>
 * Mail service interface to send mails.
 *
 * @since 1.0.0
 */
@Component
public class MailService {

  private final EMailService emailService;

  public MailService(MailProperties properties) {
    this.emailService = new EMailService(properties);
  }

  public void send(String subject, String message, String sender, String recipient) {
    emailService.send(subject, message, sender, recipient);
  }
}