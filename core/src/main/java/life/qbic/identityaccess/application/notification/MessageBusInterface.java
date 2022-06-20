package life.qbic.identityaccess.application.notification;

public interface MessageBusInterface {

  void submit(String message, MessageParameters messageParameters);

  void subscribe(MessageSubscriber subscriber, String notificationType);
}
