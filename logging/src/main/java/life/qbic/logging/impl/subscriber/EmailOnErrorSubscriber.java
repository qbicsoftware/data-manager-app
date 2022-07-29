package life.qbic.logging.impl.subscriber;

import life.qbic.logging.api.LogMessage;
import life.qbic.logging.api.Subscriber;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EmailOnErrorSubscriber implements Subscriber {

  @Override
  public void onNewMessage(LogMessage logMessage) {
    System.out.println("From the subscriber: " + logMessage.message());
  }
}
