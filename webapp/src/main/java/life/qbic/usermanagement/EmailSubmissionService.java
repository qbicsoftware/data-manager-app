package life.qbic.usermanagement;

import java.io.Serial;
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
import life.qbic.email.Email;
import life.qbic.email.EmailService;
import life.qbic.identityaccess.application.ApplicationException;
import life.qbic.shared.application.ApplicationResponse;
import org.springframework.beans.factory.annotation.Value;

/**
 * Sends emails informing a user that she was registered in the system
 *
 * @since 1.0.0
 */
public class EmailSubmissionService implements EmailService {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(
      EmailSubmissionService.class);
  @Value("${spring.mail.password}")
  private String password;

  @Value("${spring.mail.username}")
  private String userName;

  @Value("${spring.mail.host}")
  private String smtpHost;

  @Value("${spring.mail.port}")
  private Integer smtpPort;

  private ApplicationResponse sendPlainEmail(Email email) {
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
      return ApplicationResponse.failureResponse(new EmailSendingException(e.getMessage(), email));
    }
    return ApplicationResponse.successResponse();
  }

  @Override
  public void send(Email email) {
    if (email.mimeType().equals("text/plain")) {
      try {
        sendPlainEmail(email).ifSuccessOrElse(this::reportSuccess, response -> response.failures()
            .forEach(exception -> log.error(exception.getMessage(), exception)));
      } catch (Exception e) {
        log.error("Email sending failed", e.getCause());
      }
    }
  }

  private void reportSuccess(ApplicationResponse applicationResponse) {
    //ToDo Implement ApplicationEventPublisher with SendEmail Event?
  }

  public static class EmailSendingException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -8023119236306814904L;

    private final Email email;

    public EmailSendingException(String message, Email email) {
      super(message);
      this.email = email;
    }

    public Email getEmail() {
      return email;
    }
  }
}
