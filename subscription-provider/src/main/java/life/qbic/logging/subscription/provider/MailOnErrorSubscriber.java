package life.qbic.logging.subscription.provider;

import java.io.IOException;
import static java.util.Objects.requireNonNull;
import life.qbic.logging.subscription.api.LogLevel;
import life.qbic.logging.subscription.api.LogMessage;
import life.qbic.logging.subscription.api.Subscriber;
import life.qbic.logging.subscription.provider.mail.MailService;
import life.qbic.logging.subscription.provider.mail.property.MailPropertyLoader;

/**
 * Example mail on error {@link Subscriber} implementation.
 *
 * @since 1.0.0
 */
public class MailOnErrorSubscriber implements Subscriber {

  private static final String NOTIFICATION_MAIL_SENDER = "notification.mail.sender";

  private static final String NOTIFICATION_MAIL_RECIPIENT = "notification.mail.recipient";

  private final String sender;

  private final String recipient;

  private final MailService emailService;

  public MailOnErrorSubscriber() throws IOException {
    var props = MailPropertyLoader.create().load();
    sender = requireNonNull(props.getProperty(NOTIFICATION_MAIL_SENDER));
    recipient = requireNonNull(props.getProperty(NOTIFICATION_MAIL_RECIPIENT));
    emailService = MailService.instance();
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
