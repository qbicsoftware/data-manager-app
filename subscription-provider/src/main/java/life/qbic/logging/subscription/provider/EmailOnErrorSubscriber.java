package life.qbic.logging.subscription.provider;

import java.io.IOException;
import java.util.Properties;
import life.qbic.logging.subscription.api.LogMessage;
import life.qbic.logging.subscription.api.Subscriber;
import life.qbic.logging.subscription.provider.property.EmailPropertyLoader;

/**
 * Example email on error {@link Subscriber} implementation.
 *
 * @since 1.0.0
 */
public class EmailOnErrorSubscriber implements Subscriber {

  public EmailOnErrorSubscriber() {
    Properties properties;
    try {
      properties = EmailPropertyLoader.instance().load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    properties.forEach((k,v) -> System.out.printf("%s - %s\n", k, v));

  }

  @Override
  public void onMessageArrived(LogMessage logMessage) {
    System.out.println("From " + getClass().getName() + ": " + logMessage.message());
  }
}
