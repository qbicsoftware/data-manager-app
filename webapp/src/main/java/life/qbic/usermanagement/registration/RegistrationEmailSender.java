package life.qbic.usermanagement.registration;

import static org.slf4j.LoggerFactory.getLogger;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sends emails informing a user that she was registered in the system
 *
 * @since 1.0.0
 */
@Service
public class RegistrationEmailSender {

  private static final Logger log = getLogger(RegistrationEmailSender.class);

  @Value("${spring.mail.password}")
  private String password;

  @Value("${spring.mail.username}")
  private String userName;

  @Value("${spring.mail.host}")
  private String smtpHost;

  @Value("${spring.mail.port}")
  private Integer smtpPort;

  public void sendmail(String recipient, String fullName) {
    String from = "no-reply@qbic.life";

    String subject = "Activate your Data Manager Account";
    String content = RegistrationMessageFactory.registrationMessage(fullName);
    try {
      sendPlainEmail(from, recipient, subject, content);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  private void sendPlainEmail(String from, String to, String subject,
      String content) throws MessagingException {
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", smtpPort);
    log.info("smpt host is : " + props.getProperty("mail.smtp.host"));

    Session session = Session.getInstance(props, new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(userName, password);
      }
    });
    Message msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(from));
    msg.setRecipients(RecipientType.TO, InternetAddress.parse(to));
    msg.setSubject(subject);
    msg.setText(content);

    Transport.send(msg);
  }
}
