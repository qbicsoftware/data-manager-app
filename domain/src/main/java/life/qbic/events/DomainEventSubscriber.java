package life.qbic.events;

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
