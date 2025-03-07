package life.qbic.infrastructure.email.project;


import java.util.Objects;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.EmailSubmissionException;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;
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
 * @since 1.0.0
 */
public class ProjectManagementEmailServiceProvider implements EmailService {

  private static final Logger log = logger(ProjectManagementEmailServiceProvider.class);
  public static final String EMAIL_SUBMISSION_FAILED = "Email submission failed";

  private final EmailServiceProvider emailServiceProvider;

  public ProjectManagementEmailServiceProvider(EmailServiceProvider emailServiceProvider) {
    this.emailServiceProvider = Objects.requireNonNull(emailServiceProvider);
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content)
      throws CommunicationException {
    try {
      emailServiceProvider.send(MessageTranslator.translate(subject), MessageTranslator.translate(recipient),
          MessageTranslator.translate(content));
    } catch (EmailSubmissionException e) {
      log.error("Email submission failed!", e);
      throw new CommunicationException(EMAIL_SUBMISSION_FAILED);
    }
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content, Attachment attachment)
      throws CommunicationException {
    try {
      emailServiceProvider.send(MessageTranslator.translate(subject), MessageTranslator.translate(recipient),
          MessageTranslator.translate(content), MessageTranslator.translate(attachment));
    } catch (EmailSubmissionException e) {
      log.error(EMAIL_SUBMISSION_FAILED, e);
      throw new CommunicationException(EMAIL_SUBMISSION_FAILED);
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
