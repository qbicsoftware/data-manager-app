package life.qbic.projectmanagement.application.communication.broadcasting;

/**
 * <b><interface short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface MessageRouter {

  void register(Subscriber subscriber);

  void distribute(IntegrationEvent event);

}
