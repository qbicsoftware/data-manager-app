package life.qbic.infrastructure.email;

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
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import life.qbic.identity.application.communication.CommunicationException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;


/**
 * <b>Email provider</b>
 *
 * <p>Provides email infrastructure to send emails.</p>
 *
 * @since 1.0.0
 */
public class EmailServiceProvider {

  private static final Logger log = LoggerFactory.logger(EmailServiceProvider.class);
  private static final String NO_REPLY_ADDRESS = "no-reply@qbic.uni-tuebingen.de";
  private static final String NOTIFICATION_FAILED = "Notification of recipient failed!";
  private static final String SIGNATURE = """
            
      With kind regards,
            
      Your QBiC team
      """;
  private final MailServerConfiguration mailServerConfiguration;

  public EmailServiceProvider(MailServerConfiguration mailServerConfiguration) {
    this.mailServerConfiguration = Objects.requireNonNull(mailServerConfiguration);
  }

  private static Content combineMessageWithRegards(
      Content message) {
    return new Content(message.value() + SIGNATURE);
  }

  public void send(Subject subject, Recipient recipient, Content content)
      throws EmailSubmissionException {
    try {
      var message = setupMessage(subject, recipient, content);
      Transport.send(message);
      log.debug(
          "Sending email with subject %s to %s".formatted(subject.value(), recipient.address()));
    } catch (MessagingException e) {
      log.error("Could not send email to " + recipient.address(), e);
      throw new CommunicationException(NOTIFICATION_FAILED);
    }
  }

  public void send(Subject subject, Recipient recipient, Content content, Attachment attachment)
      throws EmailSubmissionException {
    try {
      var message = setupMessageWithAttachment(subject, recipient, content, attachment);
      Transport.send(message);
      log.debug(
          "Sending email with subject %s to %s".formatted(subject.value(), recipient.address()));
    } catch (MessagingException e) {
      log.error("Could not send email to " + recipient.address(), e);
      throw new CommunicationException(NOTIFICATION_FAILED);
    } catch (UnsupportedEncodingException e) {
      log.error("Unsupported encoding: " + attachment.content(), e);
      throw new CommunicationException(NOTIFICATION_FAILED);
    }
  }

  private MimeMessage setupMessageWithoutContent(
      Subject subject,
      Recipient recipient)
      throws MessagingException {
    var message = this.mailServerConfiguration.mimeMessage();
    message.setFrom(new InternetAddress(NO_REPLY_ADDRESS));
    message.setRecipient(RecipientType.TO, new InternetAddress(recipient.address()));
    message.setSubject(subject.value());
    return message;
  }

  private MimeMessage setupMessage(
      Subject subject,
      Recipient recipient,
      Content content)
      throws MessagingException {
    var message = setupMessageWithoutContent(subject, recipient);
    message.setText(combineMessageWithRegards(content).value(), StandardCharsets.UTF_8.name(),
        "plain");
    return message;
  }

  private MimeMessage setupMessageWithAttachment(Subject subject, Recipient recipient,
      Content content, Attachment attachment)
      throws MessagingException, UnsupportedEncodingException {

    var message = setupMessageWithoutContent(subject, recipient);

    BodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(combineMessageWithRegards(content).value(), "text/plain");

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(messageBodyPart);

    BodyPart attachmentPart = new MimeBodyPart();
    DataSource dataSource = new ByteArrayDataSource(attachment.content().getBytes(
        StandardCharsets.UTF_8),
        "application/octet-stream");
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName(attachment.name());
    multipart.addBodyPart(attachmentPart);

    message.setContent(multipart);
    return message;
  }

}
