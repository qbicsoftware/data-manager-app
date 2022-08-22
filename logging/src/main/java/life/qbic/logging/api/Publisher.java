package life.qbic.logging.api;

import life.qbic.logging.subscription.api.LogMessage;
import life.qbic.logging.subscription.api.Subscriber;

/**
 * Simplistic interface of a {@link Publisher} that enables subscription and unsubscription of
 * {@link Subscriber} instances that get notified when a new {@link LogMessage} is published.
 *
 * @since 1.0.0
 */
public interface Publisher {

  /**
   * Adds a {@link Subscriber} to the publisher. This method is idempotent, adding the subscriber
   * multiple times has no additional effect.
   *
   * @param s an object of type {@link Subscriber} that gets notified via its
   *          {@link Subscriber#onMessageArrived(LogMessage)} method on new messages to be published.
   * @since 1.0.0
   */
  void subscribe(Subscriber s);

  /**
   * Removes a {@link Subscriber} to the publisher. This method is idempotent, removing the
   * subscriber multiple times has no additional effect.
   *
   * @param s an object of type {@link Subscriber} that gets notified via its
   *          {@link Subscriber#onMessageArrived(LogMessage)} method on new messages to be published.
   * @since 1.0.0
   */
  void unsubscribe(Subscriber s);

  /**
   * Publishes a {@link LogMessage} via the publisher and inform all subscribers that are
   * registered.
   *
   * @param logMessage the log message to be published to all subscribers
   * @since 1.0.0
   */
  void publish(LogMessage logMessage);
}
