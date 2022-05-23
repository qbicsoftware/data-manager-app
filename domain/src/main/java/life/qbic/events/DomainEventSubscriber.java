package life.qbic.events;

import life.qbic.domain.events.DomainEvent;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@FunctionalInterface
public interface DomainEventSubscriber<T extends DomainEvent> {
    void handle(T event);
}
