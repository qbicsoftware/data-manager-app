package life.qbic.apps.datamanager.notifications;

import life.qbic.apps.datamanager.notifications.Message.MessageParameters;

public interface MessageProducer {
  void send(String notification, MessageParameters messageParameters);

}
