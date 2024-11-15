package life.qbic.infrastructure.email.identity;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import java.util.Optional;
import life.qbic.identity.application.communication.CommunicationException;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.infrastructure.email.EmailServiceProvider;
import life.qbic.infrastructure.email.EmailSubmissionException;
import life.qbic.logging.api.Logger;

/**
 * <b>Identity Email Provider</b>
 *
 * <p>Implementation of the {@link EmailService} interface to support the identity application.</p>
 *
 * @since 1.0.0
 */
public class IdentityEmailServiceProvider implements EmailService {

  private static final Logger log = logger(IdentityEmailServiceProvider.class);

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
      emailServiceProvider.send(MessageTranslator.translate(subject), MessageTranslator.translate(recipient),
          MessageTranslator.translate(content));
    } catch (EmailSubmissionException e) {
      log.error("Email submission failed!", e);
      throw new CommunicationException("Email submission failed");
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
