package life.qbic.logging.impl.publisher;

import java.util.Collection;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.slf4j.Slf4jWrapper;
import life.qbic.logging.subscription.api.Subscriber;

/**
 * Factory for creating logging publishers providing the {@link Publisher} interface.
 *
 * @since 1.0.0
 */
public class PublisherFactory {

  private static final Slf4jWrapper log = Slf4jWrapper.create(PublisherFactory.class);

  /**
   * Creates a simple publisher that includes implementations of the {@link Subscriber} interface that
   * are available in the class path during run-time.
   *
   * @return an instance of type {@link Publisher}
   * @since 1.0.0
   */
  public static Publisher createSimplePublisher() {
    var publisher = new SimplePublisher();
    var subscribers = findSubscriberImplementations();
    subscribers.forEach(publisher::subscribe);
    return publisher;
  }

  private static Collection<Subscriber> findSubscriberImplementations() {
    var subscribers = Subscriber.subscribers();
    if (subscribers.isEmpty()) {
      log.warn("No Subscriber.class provider found.");
    } else {
      log.info("Found " + subscribers.size() + " logging subscriber.");
    }
    return Subscriber.subscribers();
  }

}
