package life.qbic.apps.datamanager.notifications;

import life.qbic.apps.datamanager.notifications.Notification.MessageParameters;

public interface NotificationProducer {

  public void send(String notification, MessageParameters messageParameters);

}
