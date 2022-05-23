package life.qbic.apps.datamanager.notifications;

public interface MessageBusInterface {

    void submit(String message, MessageParameters messageParameters);

    void subscribe(MessageSubscriber subscriber, String notificationType);
}
