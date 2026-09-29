package life.qbic.infrastructure.email.usergroups;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.EmailSubmissionException;
import life.qbic.logging.api.Logger;
import life.qbic.usergroups.application.communication.CommunicationException;
import life.qbic.usergroups.application.communication.Content;
import life.qbic.usergroups.application.communication.EmailService;
import life.qbic.usergroups.application.communication.Recipient;
import life.qbic.usergroups.application.communication.Subject;

/**
 * <b>User Groups Email Provider</b>
 * <p>
 * Implementation of the user groups context's {@link EmailService} port, translating the context's
 * communication records into the shared infrastructure email model and delegating the actual
 * submission to {@link EmailServiceProvider}.
 *
 * @since 1.20.0
 */
public class UserGroupsEmailServiceProvider implements EmailService {

  private static final Logger log = logger(UserGroupsEmailServiceProvider.class);
  public static final String EMAIL_SUBMISSION_FAILED = "Email submission failed";

  private final EmailServiceProvider emailServiceProvider;

  public UserGroupsEmailServiceProvider(EmailServiceProvider emailServiceProvider) {
    this.emailServiceProvider = Objects.requireNonNull(emailServiceProvider);
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content)
      throws CommunicationException {
    try {
      emailServiceProvider.send(MessageTranslator.translate(subject),
          MessageTranslator.translate(recipient), MessageTranslator.translate(content));
    } catch (EmailSubmissionException e) {
      log.error("Email submission failed!", e);
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
  }
}