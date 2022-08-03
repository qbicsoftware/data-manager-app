package life.qbic.logging.subscription.impl;

import life.qbic.logging.subscription.api.LogMessage;
import life.qbic.logging.subscription.api.Subscriber;

/**
 * Example email on error {@link Subscriber} implementation.
 *
 * @since 1.0.0
 */
public class EmailOnErrorSubscriber implements Subscriber {

  public EmailOnErrorSubscriber() {}

  @Override
  public void onMessageArrived(LogMessage logMessage) {
    System.out.println("From " + getClass().getName() + ": " + logMessage.message());
  }
}
