package life.qbic.logging.subscription.provider.mail;

import jakarta.mail.*;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * <b>E-mail Service</b>
 * <p>
 * Implementation of the {@link MailService} that submits e-mail notifications.
 *
 * @since 1.0.0
 */
public class EMailService implements MailService {

  private static final Logger log = LoggerFactory.getLogger(EMailService.class);

  private final Session session;

  private static final String MAIL_SMTP_HOST = "mail.smtp.host";

  private static final String MAIL_SMTP_PORT = "mail.smtp.port";

  private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";

  private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

  private static final String MAIL_SMTP_PASSWORD = "mail.smtp.password";

  private static final String MAIL_SMTP_USERNAME = "mail.smtp.user";

  public static MailService create(Properties properties) {
    requireNonNull(properties.getProperty(MAIL_SMTP_HOST));
    requireNonNull(properties.getProperty(MAIL_SMTP_PORT));
    requireNonNull(properties.getProperty(MAIL_SMTP_USERNAME));
    requireNonNull(properties.getProperty(MAIL_SMTP_PASSWORD));
    requireNonNull(properties.getProperty(MAIL_SMTP_AUTH));
    requireNonNull(properties.getProperty(MAIL_SMTP_STARTTLS_ENABLE));
    return new EMailService(properties);
  }

  private EMailService(Properties props) {
    session = Session.getInstance(props, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(props.getProperty(MAIL_SMTP_USERNAME),
            props.getProperty(MAIL_SMTP_PASSWORD));
      }
    });
  }

  @Override
  public void send(String subject, String message, String sender, String recipient) {
    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(sender));
      msg.setRecipient(RecipientType.TO, new InternetAddress(recipient));
      msg.setSubject(subject);
      msg.setText(message);
      Transport.send(msg);
    } catch (MessagingException e) {
      log.error("Could not send mail after logging event", e);
    }
  }
}
