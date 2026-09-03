package life.qbic.logging.subscription.provider.mail;

import static java.util.Objects.requireNonNull;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import life.qbic.logging.subscription.provider.mail.property.MailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <b>E-mail Service</b>
 * <p>
 * Implementation of the {@link MailService} that submits e-mail notifications.
 *
 * @since 1.0.0
 */
public class EMailService {

  private static final Logger log = LoggerFactory.getLogger(EMailService.class);

  private final Session session;

  public EMailService(MailProperties properties) {
    requireNonNull(properties.getHost(), "E-mail host must not be null");
    requireNonNull(properties.getUsername(), "E-mail username must not be null");
    requireNonNull(properties.getPassword(), "E-mail password must not be null");
    session = Session.getInstance(toJavaMailProperties(properties), new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(properties.getUsername(), properties.getPassword());
      }
    });
  }

  private static Properties toJavaMailProperties(MailProperties properties) {
    Properties props = new Properties();
    props.put("mail.smtp.host", properties.getHost());
    props.put("mail.smtp.port", properties.getPort());
    props.put("mail.smtp.username", properties.getUsername());
    props.put("mail.smtp.password", properties.getPassword());
    props.put("mail.smtp.auth", properties.isSmtpAuth());
    props.put("mail.smtp.starttls.enable", properties.isStartTls());
    return props;
  }

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