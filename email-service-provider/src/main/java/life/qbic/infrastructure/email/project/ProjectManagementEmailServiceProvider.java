package life.qbic.infrastructure.email.project;


import java.util.Objects;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.EmailSubmissionException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.communication.Attachment;
import life.qbic.projectmanagement.application.communication.CommunicationException;
import life.qbic.projectmanagement.application.communication.Content;
import life.qbic.projectmanagement.application.communication.EmailService;
import life.qbic.projectmanagement.application.communication.Recipient;
import life.qbic.projectmanagement.application.communication.Subject;

/**
 * <b>Project Management Email Provider</b>
 * <p>
 * Implementation of the {@link EmailService} interface for the project management application.
 *
 * <p>Translates the shared infrastructure's {@link EmailSubmissionException} into this context's
 * {@link CommunicationException}, which is what the port declares. The catches are not decoration:
 * each context defines its own {@code CommunicationException}, so without this mapping a caller
 * catching the type its port promises would never see the failure.</p>
 *
 * <p>These blocks were unreachable for a long time, because the provider threw the <em>identity</em>
 * context's {@code CommunicationException} and never {@code EmailSubmissionException} — a different
 * class that merely shares a name.</p>
 *
 * @since 1.0.0
 */
public class ProjectManagementEmailServiceProvider implements EmailService {

  private static final Logger log = LoggerFactory.logger(ProjectManagementEmailServiceProvider.class);
  public static final String EMAIL_SUBMISSION_FAILED = "Email submission failed";

  private final EmailServiceProvider emailServiceProvider;

  public ProjectManagementEmailServiceProvider(EmailServiceProvider emailServiceProvider) {
    this.emailServiceProvider = Objects.requireNonNull(emailServiceProvider);
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content)
      throws CommunicationException {
    try {
      emailServiceProvider.send(MessageTranslator.translate(subject),
          MessageTranslator.translate(recipient), MessageTranslator.translate(content));
    } catch (EmailSubmissionException e) {
      log.error(EMAIL_SUBMISSION_FAILED, e);
      throw new CommunicationException(EMAIL_SUBMISSION_FAILED, e);
    }
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content, Attachment attachment)
      throws CommunicationException {
    try {
      emailServiceProvider.send(MessageTranslator.translate(subject),
          MessageTranslator.translate(recipient), MessageTranslator.translate(content),
          MessageTranslator.translate(attachment));
    } catch (EmailSubmissionException e) {
      log.error(EMAIL_SUBMISSION_FAILED, e);
      throw new CommunicationException(EMAIL_SUBMISSION_FAILED, e);
    }
  }

  private static class MessageTranslator {

    static life.qbic.infrastructure.email.Subject translate(Subject subject) {
      return new life.qbic.infrastructure.email.Subject(subject.content());
    }

    static life.qbic.infrastructure.email.Recipient translate(Recipient recipient) {
      return new life.qbic.infrastructure.email.Recipient(recipient.fullName(),
          recipient.address());
    }

    static life.qbic.infrastructure.email.Content translate(Content content) {
      return new life.qbic.infrastructure.email.Content(content.content());
    }

    static life.qbic.infrastructure.email.Attachment translate(Attachment attachment) {
      return new life.qbic.infrastructure.email.Attachment(attachment.name(), attachment.content());
    }


  }

}
