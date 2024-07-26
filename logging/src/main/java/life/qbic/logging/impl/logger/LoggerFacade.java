package life.qbic.logging.impl.logger;

import static java.util.Objects.requireNonNull;

import life.qbic.logging.api.Logger;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.slf4j.Slf4jWrapper;
import life.qbic.logging.subscription.api.LogLevel;
import life.qbic.logging.subscription.api.LogMessage;

/**
 * Simple implementation of the {@link Logger} interface.
 * <p>
 * This class serves as log event hook and splits the logging data flow into two new paths:
 * <p>
 * 1. The original logging intention, delegating the logging to an underlying Slf4j API call
 * <p>
 * 2. Publishing the log event to a publisher implementation that enables to account for event
 * specific business actions, for example sending an mail to the developer mailing list when an
 * error is reported.
 * <p>
 * The {@link LoggerFacade} does not contain any business logic, other than informing the publisher
 * and the logging implementation wrapper instance.
 *
 * @since 1.0.0
 */
public class LoggerFacade implements Logger {

  private final Slf4jWrapper slf4jWrapper;

  private final Publisher publisher;

  public static LoggerFacade from(String name, Publisher publisher) {
    requireNonNull(publisher, "publisher must not be null");
    return new LoggerFacade(name, publisher);
  }

  public static LoggerFacade from(Class<?> clazz, Publisher publisher) {
    requireNonNull(publisher, "publisher must not be null");
    return new LoggerFacade(clazz, publisher);
  }

  private LoggerFacade(String name, Publisher publisher) {
    this.slf4jWrapper = Slf4jWrapper.create(name);
    this.publisher = publisher;
  }

  public LoggerFacade(Class<?> clazz, Publisher publisher) {
    this.slf4jWrapper = Slf4jWrapper.create(clazz);
    this.publisher = publisher;
  }

  @Override
  public void debug(String message) {
    publish(create(LogLevel.INFO, message));
    logDebug(message);
  }

  @Override
  public void debug(String message, Throwable cause) {
    publish(create(LogLevel.DEBUG, message, cause));
    logDebug(message, cause);
  }

  private void logDebug(String message) {
    slf4jWrapper.debug(message);
  }


  @Override
  public void error(String message) {
    publish(create(LogLevel.ERROR, message));
    logError(message);
  }

  private LogMessage create(LogLevel logLevel, String message) {
    return create(logLevel, message, null);
  }

  private LogMessage create(LogLevel logLevel, String message, Throwable cause) {
    return new LogMessage(slf4jWrapper.name(), logLevel, message, cause);
  }

  @Override
  public void error(String message, Throwable cause) {
    publish(create(LogLevel.ERROR, message, cause));
    logError(message, cause);
  }

  private void logError(String message) {
    slf4jWrapper.error(message);
  }

  private void logError(String message, Throwable cause) {
    slf4jWrapper.error(message, cause);
  }

  @Override
  public void info(String message) {
    publish(create(LogLevel.INFO, message));
    logInfo(message);
  }

  private void logInfo(String message) {
    slf4jWrapper.info(message);
  }

  @Override
  public void warn(String message) {
    publish(create(LogLevel.WARNING, message));
    logWarn(message);
  }

  private void logWarn(String message) {
    slf4jWrapper.warn(message);
  }

  private void logDebug(String message, Throwable cause) {
    slf4jWrapper.debug(message, cause);
  }

  private void publish(LogMessage message) {
    publisher.publish(message);
  }
}
