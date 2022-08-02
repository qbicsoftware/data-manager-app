package life.qbic.logging.impl.subscriber.error;

import life.qbic.logging.api.LogMessage;
import life.qbic.logging.api.Subscriber;

/**
 * Example email on error {@link Subscriber} implementation.
 *
 * @since 1.0.0
 */
public class EmailOnErrorSubscriber implements Subscriber {

  @Override
  public void onMessageArrived(LogMessage logMessage) {
    System.out.println("From the subscriber: " + logMessage.message());
  }
}
