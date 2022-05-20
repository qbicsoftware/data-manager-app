package life.qbic.domain.events;

import java.util.ArrayList;
import java.util.List;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DomainEventPublisher {

  private static final ThreadLocal<List<DomainEventSubscriber<? extends DomainEvent>>> subscribers = new ThreadLocal<>();

  private static final ThreadLocal<Boolean> publishing = ThreadLocal.withInitial(
      () -> Boolean.FALSE);

  public static DomainEventPublisher instance(){
    return new DomainEventPublisher();
  }

  public DomainEventPublisher() {
    super();
  }

  public <T extends DomainEvent> void subscribe(DomainEventSubscriber<T> subscriber) {
    if (publishing.get()) {
      return;
    }
    List<DomainEventSubscriber<? extends DomainEvent>> registeredSubscribers = subscribers.get();

    if (registeredSubscribers == null) {
      registeredSubscribers = new ArrayList<>();
      subscribers.set(registeredSubscribers);
    }

    registeredSubscribers.add(subscriber);
  }

  public <T extends DomainEvent> void publish(final T domainEvent) {
    if (publishing.get()) {
      return;
    }
    try {
      publishing.set(Boolean.TRUE);
      List<DomainEventSubscriber<T>> registeredSubscribers = subscribers.get();
      Class<? extends DomainEvent> domainEventType = domainEvent.getClass();
      for (DomainEventSubscriber<T> subscriber : registeredSubscribers) {
        if (subscriber.subscribedToEventType() == domainEventType) {
          subscriber.handleEvent(domainEvent);
        }
      }
    } finally {
      publishing.set(Boolean.FALSE);
    }
  }

}
