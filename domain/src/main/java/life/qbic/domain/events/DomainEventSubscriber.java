package life.qbic.domain.events;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface DomainEventSubscriber<T> {

  Class<T> subscribedToEventType();

  void handleEvent(T event);

}
