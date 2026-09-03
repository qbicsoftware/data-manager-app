package life.qbic.logging.subscription.provider;

import life.qbic.logging.subscription.api.LogLevel;
import life.qbic.logging.subscription.api.LogMessage;
import life.qbic.logging.subscription.api.Subscriber;
import life.qbic.logging.subscription.provider.mail.MailService;
import life.qbic.logging.subscription.provider.mail.property.MailProperties;
import org.springframework.stereotype.Component;

/**
 * Example mail on error {@link Subscriber} implementation.
 *
 * @since 1.0.0
 */
@Component
public class MailOnErrorSubscriber implements Subscriber {

  private final String sender;

  private final String recipient;

  private final MailService emailService;

  public MailOnErrorSubscriber(MailProperties properties, MailService emailService) {
    this.sender = properties.getSender();
    this.recipient = properties.getRecipient();
    this.emailService = emailService;
  }

  @Override
  public void onMessageArrived(LogMessage logMessage) {
    if (logMessage.logLevel() != LogLevel.ERROR) {
      return;
    }
    emailService.send("Something went wrong!", messageContent(logMessage), sender, recipient);
  }

  private static String messageContent(LogMessage logMessage) {
    StringBuilder builder = new StringBuilder();
    builder.append(logMessage.application());
    builder.append("\n");
    builder.append(logMessage.message());
    return builder.toString();
  }
}