package life.qbic.domain.concepts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Local Domain Event Dispatcher</b>
 * <p>
 * Dispatches domain events to registered {@link DomainEventSubscriber}.
 * <p>
 * In contrast to the {@link DomainEventDispatcher} class, this class offers a
 * {@link LocalDomainEventDispatcher#reset()} method to clear all potentially existing subscribes.
 * This enables the domain event dispatcher to be used in an isolated local domain interaction
 * scope, e.g. from within an application service, when you want to ensure a successful committed
 * transaction first but still want to make use of broadcasting domain events within your domain.
 * <p>
 * <strong>Disclaimer</strong>
 * <p>The implementation runs in the main application thread and is blocking. Depending on the
 * number of registered subscriber and their implementation, expect the dispatching of events to
 * block your main app.</p>
 *
 * @since 1.0.0
 */
public class LocalDomainEventDispatcher {

  private static LocalDomainEventDispatcher INSTANCE;
  private static final ThreadLocal<List<DomainEventSubscriber<?>>> subscribers = new ThreadLocal<>();

  private LocalDomainEventDispatcher() {
    subscribers.set(new ArrayList<>());
  }

  public static LocalDomainEventDispatcher instance() {
    if (INSTANCE == null) {
      INSTANCE = new LocalDomainEventDispatcher();
    }
    return INSTANCE;
  }

  public <T extends DomainEvent> void subscribe(DomainEventSubscriber<T> subscriber) {
    var currentSubscribers = subscribers.get();
    currentSubscribers.add(subscriber);
    subscribers.set(currentSubscribers);
  }

  public <T extends DomainEvent> void dispatch(T domainEvent) {
    subscribers.get().stream()
        .filter(subscriber -> subscriber.subscribedToEventType() == domainEvent.getClass())
        .map(subscriber -> (DomainEventSubscriber<T>) subscriber)
        .forEach(subscriber -> subscriber.handleEvent(domainEvent));
  }

  /**
   * Removes all existing {@link DomainEventSubscriber}s of the dispatcher instance.
   *
   * @since 1.0.0
   */
  public void reset() {
    subscribers.set(new ArrayList<>());
  }

}
