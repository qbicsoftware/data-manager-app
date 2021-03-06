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
import life.qbic.identityaccess.domain.user.EmailAddress.EmailValidationException;
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
      return ApplicationResponse.failureResponse(
          new EmailSendingFailedException(e.getMessage(), email));
    }
    return ApplicationResponse.successResponse();
  }

  @Override
  public void send(Email email) {
    if (email.mimeType().equals("text/plain")) {
      sendPlainEmail(email).ifSuccessOrElse(
          successResponse -> reportSuccess(successResponse, email),
          failureResponse -> reportFailure(failureResponse, email));
    }
  }

  private void reportSuccess(ApplicationResponse applicationResponse, Email email) {
    String emailSendSuccessMessage = String.format(
        "Email with subject '%s' successfully send to '%s'",
        email.subject(), email.to().address());
    log.info(emailSendSuccessMessage);
  }

  private void reportFailure(ApplicationResponse applicationResponse, Email email) {
    String emailSendFailureMessage = String.format(
        "Email with subject '%s' could not be send to '%s'",
        email.subject(), email.to().address());
    applicationResponse.failures().forEach(exception -> {
      log.error(emailSendFailureMessage);
      log.error(exception.getMessage(), exception);
    });
  }

  /**
   * <h1>Exception that indicates violations during the email submission process</h1>
   *
   * <p>This exception is supposed to be thrown, if an email could not be send during the email
   * submission process It's intention is to contain the email information for which the exception
   * was thrown. This exception differs from {@link EmailValidationException}, which checks if a
   * provided email address is semantically correct
   * </p>
   * <p>
   * Example: A user provides an email address which is semantically correct but does not exist
   *
   * @since 1.0.0
   */

  public static class EmailSendingFailedException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -8023119236306814904L;
    private final Email email;

    public EmailSendingFailedException(String message, Email email) {
      super(message);
      this.email = email;
    }

    public Email getEmail() {
      return email;
    }
  }
}
