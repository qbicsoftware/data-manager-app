package life.qbic.domain.events;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Domain Event Publisher</b>
 *
 * <p>Thread-local domain event publisher class. Can be used to observe certain domain event types
 * and publish domain events within the domain.
 *
 * @since 1.0.0
 */
public class DomainEventPublisher {

  private static final ThreadLocal<List<DomainEventSubscriber<? extends DomainEvent>>> subscribers = new ThreadLocal<>();

  private static final ThreadLocal<Boolean> publishing =
      ThreadLocal.withInitial(() -> Boolean.FALSE);

  public static DomainEventPublisher instance() {
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
      List<DomainEventSubscriber<? extends DomainEvent>> registeredSubscribers = subscribers.get();
      Class<? extends DomainEvent> domainEventType = domainEvent.getClass();
      registeredSubscribers.stream()
          .filter(subscriber -> subscriber.subscribedToEventType() == domainEventType)
          .map(it -> (DomainEventSubscriber<T>) it)
          .forEach(subscriber -> subscriber.handleEvent(domainEvent));
    } finally {
      publishing.set(Boolean.FALSE);
    }
  }
}
