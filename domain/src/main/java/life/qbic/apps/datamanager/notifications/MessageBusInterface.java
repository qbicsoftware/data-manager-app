package life.qbic.apps.datamanager.notifications;

import life.qbic.apps.datamanager.notifications.Message.MessageParameters;

public interface MessageBusInterface {

  void submit(String message, MessageParameters messageParameters);

  void subscribe(MessageSubscriber subscriber, String notificationType);
}
