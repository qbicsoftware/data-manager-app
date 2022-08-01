package life.qbic.logging.impl.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves as a delegator class to delegate logging information to the respective logging function
 * offered by the Slf4j API.
 *
 * @since 1.0.0
 */
public class Slf4jWrapper {

  private final Logger logger;

  public static Slf4jWrapper create(String name) {
    return new Slf4jWrapper(LoggerFactory.getLogger(name));
  }

  public static Slf4jWrapper create(Class<?> clazz) {
    return new Slf4jWrapper(LoggerFactory.getLogger(clazz));
  }

  private Slf4jWrapper(Logger logger) {
    this.logger = logger;
  }

  public void debug(String message) {
    logger.debug(message);
  }

  ;

  public void error(String message) {
    logger.error(message);
  }

  public void error(String message, Throwable t) {
    logger.error(message, t);
  }

  public void info(String message) {
    logger.info(message);
  }

  public String name() {
    return this.logger.getName();
  }
}
