package life.qbic.domain.concepts;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Domain Event Dispatcher</b>
 * <p>
 * Dispatches domain events to registered {@link DomainEventSubscriber}.
 *
 * <p>
 * <strong>Disclaimer</strong>
 * <p>The implementation runs in the main application thread and is blocking. Depending on the
 * number of registered subscriber and their implementation, expect the dispatching of events to
 * block your main app.</p>
 *
 * @since 1.0.0
 */
public class DomainEventDispatcher {

  private static DomainEventDispatcher INSTANCE;
  private final List<DomainEventSubscriber<?>> subscribers;

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
