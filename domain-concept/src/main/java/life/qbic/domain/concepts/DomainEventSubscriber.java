package life.qbic.domain.concepts;

/**
 * <b>Domain Event Subscriber</b>
 *
 * <p>Clients can implement this interface to subscribe to {@link DomainEventDispatcher} and get
 * informed whenever a {@link DomainEvent} of the specified type <code>T</code> happens.
 *
 * @since 1.0.0
 */
public interface DomainEventSubscriber<T extends DomainEvent> {

  /**
   * Query the subscribed domain event type.
   *
   * @return the domain event type that is subscribed to
   * @since 1.0.0
   */
  Class<? extends DomainEvent> subscribedToEventType();

  /**
   * Callback that will be executed by the publisher.
   *
   * <p>Passes the domain event of the type that was subscribed to.
   *
   * @param event the domain event
   * @since 1.0.0
   */
  void handleEvent(T event);
}
