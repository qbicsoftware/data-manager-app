package life.qbic.domain.concepts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    List<DomainEventSubscriber<T>> directives = subscribers.stream()
        .filter(subscriber -> isSubscribedToEventOrSuperEvent(subscriber, domainEvent.getClass()))
        .map(subscriber -> (DomainEventSubscriber<T>) subscriber).toList();
    distinctDirectives(directives).forEach(subscriber -> subscriber.handleEvent(domainEvent));
  }

  private <T extends DomainEvent> Collection<DomainEventSubscriber<T>> distinctDirectives(
      List<DomainEventSubscriber<T>> subscribers) {
    Map<Class<?>, DomainEventSubscriber<T>> distinctDirectives = new HashMap<>();
    subscribers.forEach(directive -> distinctDirectives.put(directive.getClass(), directive));
    return distinctDirectives.values();
  }

  /**
   * Tests if a subscriber is subscribed to a specific event type or to the superclass of the event
   * This allows to listen to a number of events that have something in common, e.g. they all update
   * project information.
   */
  private boolean isSubscribedToEventOrSuperEvent(
      DomainEventSubscriber<?> subscriber, Class<? extends DomainEvent> domainEvent) {
    Class<? extends DomainEvent> listeningTo = subscriber.subscribedToEventType();
    if(listeningTo == null) {
      return false;
    }
    return listeningTo == domainEvent || listeningTo.isAssignableFrom(domainEvent);
  }

}
