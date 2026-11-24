package life.qbic.infrastructure.email.usergroups;

import java.util.Objects;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.EmailSubmissionException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
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
 * <p>Translates the shared infrastructure's {@link EmailSubmissionException} into this context's
 * {@link CommunicationException}, which is what the port declares. The catch is not decoration: each
 * context defines its own {@code CommunicationException}, so without this mapping a caller catching
 * the type its port promises would never see the failure.</p>
 *
 * <p>This block was unreachable for a long time, because the provider threw the <em>identity</em>
 * context's {@code CommunicationException} and never {@code EmailSubmissionException} — a different
 * class that merely shares a name.</p>
 *
 * @since 1.19.0
 */
public class UserGroupsEmailServiceProvider implements EmailService {

  private static final Logger log = LoggerFactory.logger(UserGroupsEmailServiceProvider.class);
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
  }
}