package life.qbic.shared.application.notification;

public interface MessageBusInterface {

  void submit(String message, MessageParameters messageParameters);

  void subscribe(MessageSubscriber subscriber, String notificationType);
}
