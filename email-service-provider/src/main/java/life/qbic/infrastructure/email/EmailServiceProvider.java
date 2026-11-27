package life.qbic.infrastructure.email;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;


/**
 * <b>Email provider</b>
 *
 * <p>Provides email infrastructure to send emails.</p>
 *
 * <h3>Two exception families, and why both are caught</h3>
 *
 * <p>Message <em>assembly</em> and message <em>submission</em> fail with unrelated exception types,
 * and catching only one of them is how a failed send became invisible:</p>
 *
 * <ul>
 *   <li>{@link MessagingException} — thrown while building the message, e.g.
 *       {@code AddressException} for a malformed recipient.</li>
 *   <li>{@link MailException} — thrown by {@code JavaMailSenderImpl} on every transport failure,
 *       as {@code MailSendException} (connection refused, TLS refused, timeout) or
 *       {@code MailAuthenticationException} (rejected credentials).</li>
 * </ul>
 *
 * <p>{@code MailException} does <b>not</b> extend {@code MessagingException} — the two hierarchies
 * are independent. This class used to call {@code Transport.send(message)} and therefore declared
 * {@code catch (MessagingException)}, which was correct then. When the submission moved to Spring's
 * {@code JavaMailSender} the call changed but the handler did not, so from that point on every
 * genuine submission failure propagated out of here <b>unlogged</b> and untranslated.</p>
 *
 * <p>That is not a cosmetic gap. A transient SMTP fault then surfaced only as a background job that
 * silently retried, with no error log, no operator notification, and nothing distinguishing it from
 * a broken release. The specs did not catch it because they exercise a malformed address — which is
 * an {@code AddressException}, i.e. the one failure mode that <em>was</em> still handled.</p>
 *
 * <h3>What this class throws, and why it is not a context type</h3>
 *
 * <h3>Why the attachment overload does not log</h3>
 *
 * <p>Only the plain-text overload logs the cause. The attachment overload throws the same type with
 * the same cause chain and leaves logging to its caller, because it runs inside a JobRunr job whose
 * failure handler prints the whole chain (see {@code EmailFailureIsVisibleSpec}). Logging in both
 * places would put the same stack trace in the log twice: once attributed to the sending thread and
 * once to the job's failure.</p>
 *
 * <p>Both handlers throw {@link EmailSubmissionException}. This provider is shared infrastructure
 * used by the identity, project-management and user-groups contexts, and each of those defines its
 * <em>own</em> {@code CommunicationException} — three distinct classes sharing a name. Throwing one
 * context's type from here would break the other two, because their callers catch their own. Each
 * adapter under {@code infrastructure.email.<context>} therefore maps this type onto the exception
 * its port declares.</p>
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
  private final JavaMailSender mailSender;

  public EmailServiceProvider(JavaMailSender mailSender) {
    this.mailSender = Objects.requireNonNull(mailSender);
  }

  private static Content combineMessageWithRegards(
      Content message) {
    return new Content(message.value() + SIGNATURE);
  }

  public void send(Subject subject, Recipient recipient, Content content)
      throws EmailSubmissionException {
    try {
      var message = setupMessage(subject, recipient, content);
      mailSender.send(message);
      log.debug(
          "Sending email with subject %s to %s".formatted(subject.value(), recipient.address()));
    } catch (MessagingException e) {
      // Assembly failed - a malformed recipient address reaches here.
      log.error("Could not prepare email to " + recipient.address(), e);
      throw new EmailSubmissionException(NOTIFICATION_FAILED, e);
    } catch (MailException e) {
      // Submission failed. This is the clause that keeps a broken or unreachable mail server
      // visible: without it the exception escaped unlogged and the only symptom was a background
      // job quietly retrying (see the class comment).
      log.error("Could not submit email to " + recipient.address(), e);
      throw new EmailSubmissionException(NOTIFICATION_FAILED, e);
    }
  }

  public void send(Subject subject, Recipient recipient, Content content, Attachment attachment)
      throws EmailSubmissionException {
    try {
      var message = setupMessageWithAttachment(subject, recipient, content, attachment);
      mailSender.send(message);
      log.debug(
          "Sending email with subject %s to %s".formatted(subject.value(), recipient.address()));
    } catch (MessagingException e) {
      // Assembly failed - a malformed recipient address reaches here.
      log.error("Could not prepare email to " + recipient.address(), e);
      throw new EmailSubmissionException(NOTIFICATION_FAILED, e);
    } catch (MailException e) {
      // Submission failed. Not logged here on purpose - see the class comment; the cause is attached
      // and the job failure handler prints it, so the stack trace is not duplicated in the log.
      throw new EmailSubmissionException(NOTIFICATION_FAILED, e);
    }
  }

  private MimeMessage setupMessageWithoutContent(
      Subject subject,
      Recipient recipient)
      throws MessagingException {
    var message = this.mailSender.createMimeMessage();
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
      throws MessagingException {

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
