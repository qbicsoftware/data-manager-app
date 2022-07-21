package life.qbic.identity.application.notification;

public interface MessageBusSubmission {

  void submit(String message, MessageParameters messageParameters);
}
