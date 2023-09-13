package life.qbic.newshandler.usermanagement.email;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;
import life.qbic.domain.concepts.communication.CommunicationException;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b>Email communication service</b>
 *
 * <p>Implementation of a {@link CommunicationService} that uses email to communicate with the
 * user.</p>
 *
 * @since 1.0.0
 */
public class EmailCommunicationService implements CommunicationService {

  private static final Logger log = LoggerFactory.logger(
      EmailCommunicationService.class);
  private static final String NO_REPLY_ADDRESS = "no-reply@qbic.uni-tuebingen.de";

  private static final String SIGNATURE = """
      \nWith kind regards,
            
      Your QBiC team
      """;

  private final MailServerConfiguration mailServerConfiguration;

  public EmailCommunicationService(MailServerConfiguration mailServerConfiguration) {
    this.mailServerConfiguration = Objects.requireNonNull(mailServerConfiguration);
  }

  private static Content combineMessageWithRegards(Content message) {
    return new Content(message.content() + SIGNATURE);
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content) {
    try {
      var message = setupMessage(subject, recipient, content);
      Transport.send(message);
      log.debug(
          "Sending email with subject %s to %s".formatted(subject.content(), recipient.address()));
    } catch (MessagingException e) {
      log.error("Could not send email to " + recipient.address(), e);
      throw new CommunicationException("Notification of recipient failed!");
    }
  }

  private MimeMessage setupMessage(Subject subject, Recipient recipient, Content content)
      throws MessagingException {
    var message = this.mailServerConfiguration.mimeMessage();
    message.setFrom(new InternetAddress(NO_REPLY_ADDRESS));
    message.setContent(combineMessageWithRegards(content), "text/plain");
    message.setRecipient(RecipientType.TO, new InternetAddress(recipient.address()));
    message.setSubject(subject.content());
    return message;
  }
}
