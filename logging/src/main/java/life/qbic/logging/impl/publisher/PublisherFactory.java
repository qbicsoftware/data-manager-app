package life.qbic.logging.impl.publisher;

import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.subscriber.error.EmailOnErrorSubscriber;

/**
 * Factory for creating logging publishers providing the {@link Publisher} interface.
 *
 * @since 1.0.0
 */
public class PublisherFactory {

  /**
   * Creates a simple publisher that includes a {@link EmailOnErrorSubscriber} subscription.
   *
   * @return an instance of type {@link Publisher}
   * @since 1.0.0
   */
  public static Publisher createSimplePublisher() {
    var publisher = new SimplePublisher();
    var emailOnErrorSubscriber = new EmailOnErrorSubscriber();
    publisher.subscribe(emailOnErrorSubscriber);
    return publisher;
  }

}
