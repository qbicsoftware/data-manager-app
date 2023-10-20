package life.qbic.infrastructure.email.project;


import java.util.Objects;
import life.qbic.infrastructure.email.EmailProvider;
import life.qbic.projectmanagement.application.communication.Attachment;
import life.qbic.projectmanagement.application.communication.CommunicationException;
import life.qbic.projectmanagement.application.communication.Content;
import life.qbic.projectmanagement.application.communication.EmailService;
import life.qbic.projectmanagement.application.communication.Recipient;
import life.qbic.projectmanagement.application.communication.Subject;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectManagementEmailProvider implements EmailService {


  private final EmailProvider emailProvider;

  public ProjectManagementEmailProvider(EmailProvider emailProvider) {
    this.emailProvider = Objects.requireNonNull(emailProvider);
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content)
      throws CommunicationException {
    emailProvider.send(Translator.translate(subject), Translator.translate(recipient),
        Translator.translate(content));
  }

  @Override
  public void send(Subject subject, Recipient recipient, Content content, Attachment attachment)
      throws CommunicationException {
    emailProvider.send(Translator.translate(subject), Translator.translate(recipient),
        Translator.translate(content), Translator.translate(attachment));
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

    static life.qbic.infrastructure.email.Attachment translate(Attachment attachment) {
      return new life.qbic.infrastructure.email.Attachment(attachment.name(), attachment.content());
    }


  }

}
