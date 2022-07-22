package life.qbic.broadcasting;

public interface MessageSubscription {

  void subscribe(MessageSubscriber subscriber, String notificationType);

}
