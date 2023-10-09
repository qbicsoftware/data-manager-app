package life.qbic.newshandler.usermanagement.email;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import life.qbic.domain.concepts.communication.Attachment;
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
      
      With kind regards,
            
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

  @Override
  public void send(Subject subject, Recipient recipient, Content content, Attachment attachment) {
    try {
      var message = setupMessageWithAttachment(subject, recipient, content, attachment);
      Transport.send(message);
      log.debug(
          "Sending email with subject %s to %s".formatted(subject.content(), recipient.address()));
    } catch (MessagingException e) {
      log.error("Could not send email to " + recipient.address(), e);
      throw new CommunicationException("Notification of recipient failed!");
    } catch (UnsupportedEncodingException e) {
      log.error("Could not create attachment for email to " + recipient.address(), e);
      throw new CommunicationException("Notification of recipient failed!");
    }
  }

  private MimeMessage setupMessageWithoutContent(Subject subject, Recipient recipient)
      throws MessagingException {
    var message = this.mailServerConfiguration.mimeMessage();
    message.setFrom(new InternetAddress(NO_REPLY_ADDRESS));
    message.setRecipient(RecipientType.TO, new InternetAddress(recipient.address()));
    message.setSubject(subject.content());
    return message;
  }

  private MimeMessage setupMessage(Subject subject, Recipient recipient, Content content)
      throws MessagingException {
    var message = setupMessageWithoutContent(subject, recipient);
    message.setContent(combineMessageWithRegards(content).content(), "text/plain");
    return message;
  }

  private MimeMessage setupMessageWithAttachment(Subject subject, Recipient recipient,
      Content content, Attachment attachment)
      throws MessagingException, UnsupportedEncodingException {

    var message = setupMessageWithoutContent(subject, recipient);

    BodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(combineMessageWithRegards(content).content(), "text/plain");

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(messageBodyPart);

    BodyPart attachmentPart = new MimeBodyPart();
    DataSource dataSource = new ByteArrayDataSource(attachment.content().getBytes("UTF-8"),
        "application/octet-stream");
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName(attachment.name());
    multipart.addBodyPart(attachmentPart);

    message.setContent(multipart);
    return message;
  }

}
