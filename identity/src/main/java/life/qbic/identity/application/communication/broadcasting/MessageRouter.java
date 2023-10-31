package life.qbic.identity.application.communication.broadcasting;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MessageRouter {

  void register(MessageTypeSubscriber messageTypeSubscriber);

  void dispatch(IntegrationEvent event);

}
