package life.qbic.projectmanagement.infrastructure.communication;

import java.util.LinkedList;
import java.util.List;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import life.qbic.projectmanagement.application.communication.broadcasting.Subscriber;
import org.springframework.stereotype.Component;

/**
 * <b>Message Router implementation</b>
 *
 * <p>Implementation of the {@link MessageRouter} interface.</p>
 * <p>
 * Manages the registered subscribers and dispatches events to interested subscribers based on the
 * event type they are interested in.
 *
 * @since 1.0.0
 */
@Component
public class MessageRouterImpl implements MessageRouter {

  private final List<Subscriber> subscribers;

  public MessageRouterImpl() {
    this.subscribers = new LinkedList<>();
  }

  /**
   * @inheritDocs
   */
  @Override
  public void register(Subscriber subscriber) {
    this.subscribers.add(subscriber);
  }

  /**
   * @inheritDocs
   */
  @Override
  public void dispatch(IntegrationEvent event) {
    var eventType = event.type();
    subscribers.stream().filter(subscriber -> subscriber.type().equals(eventType))
        .forEach(subscriber -> subscriber.onReceive(event));
  }
}
