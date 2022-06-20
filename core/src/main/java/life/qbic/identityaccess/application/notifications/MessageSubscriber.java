package life.qbic.identityaccess.application.notifications;

/**
 * Interface for notification subscribers. This interface needs to be implemented, when a client
 * wants to subscribe to certain event types.
 *
 * <p>This follows the classic Publish/Subscribe pattern using an exchange instance to decouple the
 * publisher from the subscribers.
 *
 * @since 1.0.0
 */
public interface MessageSubscriber {

  /**
   * Receive notifications that are passed from clients such as i.e. an exchange to broadcast
   * notifications.
   *
   * @param message a message with encoded information.
   * @since 1.0.0
   */
  void receive(String message, MessageParameters messageParameters);
}
