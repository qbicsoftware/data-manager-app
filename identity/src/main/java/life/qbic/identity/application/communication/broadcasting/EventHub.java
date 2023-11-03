package life.qbic.identity.application.communication.broadcasting;

/**
 * <b>Event Hub</b>
 * <p>
 * Interface to broadcast events outside the own domain.
 *
 * @since 1.0.0
 */
public interface EventHub {

  /**
   * Sends an {@link IntegrationEvent} to the event hub, from where it will get broadcast.
   *
   * @param event the integration event to broadcast
   * @since 1.0.0
   */
  void send(IntegrationEvent event);

}
