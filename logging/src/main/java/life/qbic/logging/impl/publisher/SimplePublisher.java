package life.qbic.logging.impl.publisher;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import life.qbic.logging.api.LogMessage;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.api.Subscriber;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class SimplePublisher implements Publisher {

  private final Set<Subscriber> subscribers;

  public SimplePublisher() {
    subscribers = new HashSet<>();
  }

  @Override
  public void subscribe(Subscriber s) {
    Objects.requireNonNull(s, "Subscriber must not be null");
    subscribers.add(s);
  }

  @Override
  public void unsubscribe(Subscriber s) {
    Objects.requireNonNull(s, "Subscriber must not be null");
    subscribers.remove(s);
  }

  @Override
  public void publish(LogMessage logMessage) {
    Objects.requireNonNull(logMessage, "LogMessage must not be null");
    subscribers.forEach((s) -> s.onNewMessage(logMessage));
  }
}
