package life.qbic.logging.impl.logger;

import java.util.Objects;
import life.qbic.logging.api.LogLevel;
import life.qbic.logging.api.LogMessage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.slf4j.Slf4jWrapper;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class LoggerFacade implements Logger {

  private final Slf4jWrapper slf4jWrapper;

  private final Publisher publisher;

  public static LoggerFacade from(Class<?> clazz, Publisher publisher) {
    Objects.requireNonNull(publisher);
    return new LoggerFacade(clazz, publisher);
  }

  private LoggerFacade(Class<?> clazz, Publisher publisher) {
    this.slf4jWrapper = Slf4jWrapper.create(clazz);
    this.publisher = publisher;
  }

  @Override
  public void debug(String message) {
    publish(create(LogLevel.INFO, message));
    logDebug(message);
  }

  @Override
  public void error(String message) {
    publish(create(LogLevel.ERROR, message));
    logError(message);
  }

  private LogMessage create(LogLevel logLevel, String message) {
    return create(logLevel, message, null);
  }

  private LogMessage create(LogLevel logLevel, String message, Throwable t) {
    return new LogMessage(slf4jWrapper.name(), logLevel, message, t);
  }

  @Override
  public void error(String message, Throwable t) {
    publish(create(LogLevel.ERROR, message, t));
  }

  @Override
  public void info(String message) {
    publish(create(LogLevel.INFO, message));
    logInfo(message);
  }


  private void logDebug(String message) {
    slf4jWrapper.debug(message);
  }

  private void logError(String message) {
    slf4jWrapper.error(message);
  }

  private void logInfo(String message) {
    slf4jWrapper.info(message);
  }

  private void publish(LogMessage message) {
    publisher.publish(message);
  }
}
