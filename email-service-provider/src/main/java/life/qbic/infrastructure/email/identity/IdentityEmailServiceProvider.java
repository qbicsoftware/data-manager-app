package life.qbic.infrastructure.email.identity;

import java.util.Objects;
import java.util.Optional;
import life.qbic.identity.application.communication.CommunicationException;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.EmailSubmissionException;

/**
 * <b>Identity Email Provider</b>
 *
 * <p>Implementation of the {@link EmailService} interface to support the identity application.</p>
 *
 * <p>Translates the shared infrastructure's {@link EmailSubmissionException} into this context's
 * {@link CommunicationException}, which is what the port declares. Each context has its own
 * {@code CommunicationException} type, so this mapping is what makes the port's contract true rather
 * than nominal — a caller catching this context's type must actually receive it.</p>
 *
 * @since 1.0.0
 */
public class IdentityEmailServiceProvider implements EmailService {

  private final EmailServiceProvider emailServiceProvider;

  public IdentityEmailServiceProvider(EmailServiceProvider emailServiceProvider) {
    this.emailServiceProvider = Objects.requireNonNull(emailServiceProvider);
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content)
      throws CommunicationException {
    subject = Optional.ofNullable(subject)
        .orElseThrow(() -> new CommunicationException("No subject provided."));
    recipient = Optional.ofNullable(recipient)
        .orElseThrow(() -> new CommunicationException("No recipient provided."));
    content = Optional.ofNullable(content)
        .orElseThrow(() -> new CommunicationException("No content provided."));

    try {
      emailServiceProvider.send(MessageTranslator.translate(subject),
          MessageTranslator.translate(recipient), MessageTranslator.translate(content));
    } catch (EmailSubmissionException e) {
      // The provider has already logged the underlying cause with full detail; this keeps the
      // chain so nothing is lost, and hands the caller the type its port promises.
      throw new CommunicationException(e.getMessage(), e);
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
