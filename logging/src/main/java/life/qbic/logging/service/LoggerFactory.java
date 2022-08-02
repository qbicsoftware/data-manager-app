package life.qbic.logging.service;

import life.qbic.logging.api.Logger;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.logger.LoggerFacade;
import life.qbic.logging.impl.publisher.PublisherFactory;

/**
 * Factory to create {@link Logger} objects that can then be used to log events on different log
 * levels.
 *
 * @since 1.0.0
 */
public class LoggerFactory {

  private static final Publisher publisher = PublisherFactory.createSimplePublisher();

  /**
   * Creates a simple logger that carries the provided name.
   *
   * @param name the logger instance name provided by the client
   * @return a {@link Logger} instance
   * @since 1.0.0
   */
  public static Logger logger(String name) {
    return LoggerFacade.from(name, publisher);
  }

  /**
   * Creates a simple logger that carries the provided name.
   *
   * @param clazz the {@link Class} for which the logger is intended to be used.
   * @return a {@link Logger} instance
   * @since 1.0.0
   */
  public static Logger logger(Class<?> clazz) {
    return LoggerFacade.from(clazz, publisher);
  }
}
