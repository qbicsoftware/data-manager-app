package life.qbic.logging.api;

public interface Publisher {

  void subscribe(Subscriber s);

  void unsubscribe(Subscriber s);

  void publish(LogMessage logMessage);
}
