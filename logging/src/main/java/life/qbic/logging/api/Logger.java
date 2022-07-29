package life.qbic.logging.api;

public interface Logger {

  void debug(String message);

  void error(String message);

  void error(String message, Throwable t);

  void info(String message);

}
