package life.qbic.broadcasting;

public interface MessageBusSubmission {

  void submit(String message, MessageParameters messageParameters);
}
