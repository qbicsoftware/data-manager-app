package life.qbic.apps.datamanager.notifications;

/**
 * Interface for notification subscribers. This interface needs to be implemented, when a client
 * wants to subscribe to certain event types in the {@link Exchange}.
 * <p>
 * This follows the classic Publish/Subscribe pattern using an exchange instance to decouple the
 * publisher from the subscribers.
 *
 * @since 1.0.0
 */
public interface NotificationSubscriber {

  /**
   * Receive notifications that are passed from clients such as i.e. an {@link Exchange} to
   * broadcast notifications.
   *
   * @param notification a notification with encoded information. See {@link Notification} for more
   *                     information.
   * @since 1.0.0
   */
  void receive(Notification notification);

}
