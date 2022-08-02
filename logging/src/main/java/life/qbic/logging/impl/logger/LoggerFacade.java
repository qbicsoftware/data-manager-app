package life.qbic.logging.impl.logger;

import java.util.Objects;
import life.qbic.logging.api.LogLevel;
import life.qbic.logging.api.LogMessage;
import life.qbic.logging.api.Logger;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.slf4j.Slf4jWrapper;

/**
 * Simple implementation of the {@link Logger} interface.
 * <p>
 * This class serves as log event hook and splits the logging data flow into two new paths:
 * <p>
 * 1. The original logging intention, delegating the logging to an underlying Slf4j API call
 * <p>
 * 2. Publishing the log event to a publisher implementation that enables to account for event
 * specific business actions, for example sending an email to the developer mailing list when an
 * error is reported.
 *
 * The {@link LoggerFacade} does not contain any business logic, other than informing the publisher
 * and the logging implementation wrapper instance.
 *
 * @since 1.0.0
 */
public class LoggerFacade implements Logger {

  private final Slf4jWrapper slf4jWrapper;

  private final Publisher publisher;

  public static LoggerFacade from(String name, Publisher publisher) {
    Objects.requireNonNull(publisher);
    return new LoggerFacade(name, publisher);
  }

  public static LoggerFacade from(Class<?> clazz, Publisher publisher) {
    Objects.requireNonNull(publisher);
    return from(clazz.getName(), publisher);
  }

  private LoggerFacade(String name, Publisher publisher) {
    this.slf4jWrapper = Slf4jWrapper.create(name);
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

  private LogMessage create(LogLevel logLevel, String message, Throwable cause) {
    return new LogMessage(slf4jWrapper.name(), logLevel, message, cause);
  }

  @Override
  public void error(String message, Throwable cause) {
    publish(create(LogLevel.ERROR, message, cause));
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
