package life.qbic.newshandler.usermanagement.email;

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
import org.springframework.beans.factory.annotation.Value;

/**
 * Sends emails informing a user that she was registered in the system
 *
 * @since 1.0.0
 */
public class EmailSubmissionService implements EmailService {

  @Value("${spring.mail.password}")
  private String password;

  @Value("${spring.mail.username}")
  private String userName;

  @Value("${spring.mail.host}")
  private String smtpHost;

  @Value("${spring.mail.port}")
  private Integer smtpPort;

  private void sendPlainEmail(Email email) {
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", smtpPort);

    try {
      Session session = Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(userName, password);
        }
      });
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(email.from()));
      msg.setRecipient(RecipientType.TO, new InternetAddress(email.to().address()));
      msg.setSubject(email.subject());
      msg.setText(email.content());

      Transport.send(msg);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void send(Email email) {
    if (email.mimeType().equals("text/plain")) {
      sendPlainEmail(email);
    }
  }
}
