package life.qbic.logging.service;

import life.qbic.logging.api.Logger;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.logger.LoggerFacade;
import life.qbic.logging.impl.publisher.PublisherFactory;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class LoggerFactory {

  private static final Publisher publisher = PublisherFactory.createSimplePublisher();

  public static Logger logger(String name) {
    return LoggerFacade.from(name, publisher);
  }

  public static Logger logger(Class<?> clazz) {
    return LoggerFacade.from(clazz, publisher);
  }
}
