package life.qbic.domain.events;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>Domain Event Producer</b>
 * <p>
 * Local domain event producer class. Can be used to observe certain domain event types and publish domain events
 * within the domain.
 *
 * @since 1.0.0
 */
public class DomainEventProducer {

    private static final ThreadLocal<List> subscribers = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> publishing = ThreadLocal.withInitial(
            () -> Boolean.FALSE);

    public static DomainEventProducer instance() {
        return new DomainEventProducer();
    }

    public DomainEventProducer() {
        super();
    }

    public <T extends DomainEvent> void subscribe(DomainEventSubscriber<T> subscriber) {
        if (publishing.get()) {
            return;
        }
        List<DomainEventSubscriber<T>> registeredSubscribers = subscribers.get();

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
