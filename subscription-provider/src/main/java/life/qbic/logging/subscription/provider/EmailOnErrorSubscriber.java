package life.qbic.logging.subscription.provider;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.util.Objects;
import life.qbic.logging.subscription.api.LogLevel;
import life.qbic.logging.subscription.api.LogMessage;
import life.qbic.logging.subscription.api.Subscriber;
import life.qbic.logging.subscription.provider.email.EmailService;
import life.qbic.logging.subscription.provider.email.property.EmailPropertyLoader;

/**
 * Example email on error {@link Subscriber} implementation.
 *
 * @since 1.0.0
 */
public class EmailOnErrorSubscriber implements Subscriber {

  private static final String NOTIFICATION_MAIL_SENDER = "notification.mail.sender";

  private static final String NOTIFICATION_MAIL_RECIPIENT = "notification.mail.recipient";

  private final String sender;

  private final String recipient;

  private final EmailService emailService;
  public EmailOnErrorSubscriber() throws IOException {
    var props = EmailPropertyLoader.create().load();
    sender = requireNonNull(props.getProperty(NOTIFICATION_MAIL_SENDER));
    recipient = requireNonNull(props.getProperty(NOTIFICATION_MAIL_RECIPIENT));
    emailService = EmailService.instance();
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
