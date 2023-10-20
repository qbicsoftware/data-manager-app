package life.qbic.infrastructure.email.identity;

import java.util.Objects;
import java.util.Optional;
import life.qbic.identity.application.communication.CommunicationException;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.infrastructure.email.EmailProvider;
import life.qbic.infrastructure.email.EmailSubmissionException;

/**
 * <b>Identity Email Provider</b>
 *
 * <p>Implementation of the {@link EmailService} interface to support the identity application.</p>
 *
 * @since 1.0.0
 */
public class IdentityEmailProvider implements EmailService {

  private final EmailProvider emailProvider;

  public IdentityEmailProvider(EmailProvider emailProvider) {
    this.emailProvider = Objects.requireNonNull(emailProvider);
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content)
      throws CommunicationException {
    Optional.ofNullable(subject)
        .orElseThrow(() -> new CommunicationException("No subject provided."));
    Optional.ofNullable(recipient)
        .orElseThrow(() -> new CommunicationException("No recipient provided."));
    Optional.ofNullable(content)
        .orElseThrow(() -> new CommunicationException("No content provided."));

    try {
      emailProvider.send(Translator.translate(subject), Translator.translate(recipient),
          Translator.translate(content));
    } catch (EmailSubmissionException e) {
      // TODO log
      throw new CommunicationException("Email submission failed");
    }
  }

  private static class Translator {

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
