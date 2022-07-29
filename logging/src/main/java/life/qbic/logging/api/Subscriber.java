package life.qbic.logging.api;

public interface Subscriber {

  void onNewMessage(LogMessage logMessage);

}
