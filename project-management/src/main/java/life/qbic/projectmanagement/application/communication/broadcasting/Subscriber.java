package life.qbic.projectmanagement.application.communication.broadcasting;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public interface Subscriber {

  String topic();

  void onMatchingTopic(IntegrationEvent event);

}
