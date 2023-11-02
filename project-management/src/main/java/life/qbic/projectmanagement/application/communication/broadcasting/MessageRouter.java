package life.qbic.projectmanagement.application.communication.broadcasting;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
/**
 * <b>Message Router</b>
 * <p>
 * Message routers sit in the infrastructure layer of the application and forward any incoming
 * integration events to interested subscribers.
 * <p>
 * The router is also the interface to the broadcasting implementation, integrating multiple domain
 * events.
 *
 * @since 1.0.0
 */
public interface MessageRouter {

  /**
   * Registers a {@link Subscriber} to the message router. On matching incoming events that have
   * equal types based on the {@link Subscriber#type()}, the
   * {@link Subscriber#onReceive(IntegrationEvent)} callback gets executed.
   *
   * @param subscriber the subscriber to register in the router
   * @since 1.0.0
   */
  void register(Subscriber subscriber);

  /**
   * Dispatches an event to leave the current domain and broadcasts it using a messaging
   * middleware.
   *
   * @param event the integration event to broadcast out of the domain
   * @since 1.0.0
   */
  void dispatch(IntegrationEvent event);

}
