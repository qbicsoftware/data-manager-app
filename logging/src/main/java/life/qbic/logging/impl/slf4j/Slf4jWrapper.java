package life.qbic.logging.impl.slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Slf4jWrapper {

  private final Logger logger;

  public static Slf4jWrapper create(Class<?> clazz) {
    return new Slf4jWrapper(LoggerFactory.getLogger(clazz));
  }

  private Slf4jWrapper(Logger logger) {
    this.logger = logger;
  }

  public void debug(String message){
    logger.debug(message);
  };

  public void error(String message){
    logger.error(message);
  }

  public void error(String message, Throwable t){
    logger.error(message, t);
  }

  public void info(String message){
    logger.info(message);
  }

  public String name() {
    return this.logger.getName();
  }
}
