package life.qbic.logging.subscription.provider.email;

import static java.util.Objects.requireNonNull;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SimpleEmailService implements EmailService {

  private static final Logger log = LoggerFactory.getLogger(SimpleEmailService.class);

  private final Session session;

  private static final String MAIL_SMTP_HOST = "mail.smtp.host";

  private static final String MAIL_SMTP_PORT = "mail.smtp.port";

  private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";

  private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

  private static final String MAIL_SMTP_PASSWORD = "mail.smtp.password";

  private static final String MAIL_SMTP_USERNAME = "mail.smtp.user";

  public static EmailService create(Properties properties) {
    requireNonNull(properties.getProperty(MAIL_SMTP_HOST));
    requireNonNull(properties.get(MAIL_SMTP_PORT));
    requireNonNull(properties.getProperty(MAIL_SMTP_USERNAME));
    requireNonNull(properties.get(MAIL_SMTP_PASSWORD));
    requireNonNull(properties.get(MAIL_SMTP_AUTH));
    requireNonNull(properties.get(MAIL_SMTP_STARTTLS_ENABLE));
    return new SimpleEmailService(properties);
  }

  public SimpleEmailService(Properties props) {
    session = Session.getDefaultInstance(props, new Authenticator() {
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
