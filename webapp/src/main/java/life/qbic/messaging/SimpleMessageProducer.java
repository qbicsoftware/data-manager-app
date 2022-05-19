package life.qbic.messaging;

import life.qbic.apps.datamanager.notifications.Message.MessageParameters;
import life.qbic.apps.datamanager.notifications.MessageProducer;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SimpleMessageProducer implements MessageProducer {
  private static SimpleMessageProducer instance;

  private final Exchange exchange;

  public static SimpleMessageProducer instance(Exchange exchange) {
    if(instance == null) {
      instance = new SimpleMessageProducer(exchange);
    }
    return instance;
  }

  protected SimpleMessageProducer(Exchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public void send(String notification, MessageParameters messageParameters) {
    exchange.submit(notification, messageParameters);
  }
}
