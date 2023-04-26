package life.qbic.domain.concepts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DomainEventDispatcher {

  private static DomainEventDispatcher INSTANCE;
  private final List<DomainEventSubscriber<? extends DomainEvent>> subscribers;

  private DomainEventDispatcher() {
    subscribers = new ArrayList<>();
  }

  public static DomainEventDispatcher instance() {
    if (INSTANCE == null) {
      INSTANCE = new DomainEventDispatcher();
    }
    return INSTANCE;
  }

  public <T extends DomainEvent> void subscribe(DomainEventSubscriber<T> subscriber) {
    this.subscribers.add(subscriber);
  }

  public <T extends DomainEvent> void dispatch(T domainEvent) {
    subscribers.stream()
        .filter(subscriber -> subscriber.subscribedToEventType() == domainEvent.getClass())
        .map(subscriber -> (DomainEventSubscriber<T>) subscriber)
        .forEach(subscriber -> subscriber.handleEvent(domainEvent));
  }

}
