package life.qbic.identityaccess.application.notification;

public interface MessageBusSubmission {

  void submit(String message, MessageParameters messageParameters);
}
