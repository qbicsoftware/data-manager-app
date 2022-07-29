package life.qbic.logging.impl.publisher;

import java.util.Objects;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.impl.subscriber.EmailOnErrorSubscriber;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class PublisherFactory {
  public static Publisher createSimplePublisher() {
    var publisher = new SimplePublisher();
    var emailOnErrorSubscriber = new EmailOnErrorSubscriber();
    publisher.subscribe(emailOnErrorSubscriber);
    return publisher;
  }

}
