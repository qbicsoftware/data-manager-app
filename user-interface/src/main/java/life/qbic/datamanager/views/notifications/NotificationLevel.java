package life.qbic.datamanager.views.notifications;

/**
 * The level of a notification
 * <ol>
 *   <li><strong>SUCCESS</strong>: The notification informs of a successful operation</li>
 *   <li><strong>INFO</strong>: The notification is used solely to convey information</li>
 *   <li><strong>WARNING</strong>: The notification warns the user of something</li>
 *   <li><strong>ERROR</strong>: The notification informs about an error </li>
 * </ol>
 */
public enum NotificationLevel {
  SUCCESS, INFO, WARNING, ERROR
}
