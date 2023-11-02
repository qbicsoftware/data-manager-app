package life.qbic.projectmanagement.infrastructure.communication;

import java.util.LinkedList;
import java.util.List;
import life.qbic.projectmanagement.application.communication.broadcasting.IntegrationEvent;
import life.qbic.projectmanagement.application.communication.broadcasting.MessageRouter;
import life.qbic.projectmanagement.application.communication.broadcasting.Subscriber;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class MessageRouterImpl implements MessageRouter {

  private final List<Subscriber> subscribers;

  public MessageRouterImpl() {
    this.subscribers = new LinkedList<>();
  }

  @Override
  public void register(Subscriber subscriber) {
    this.subscribers.add(subscriber);
  }

  @Override
  public void distribute(IntegrationEvent event) {
    var eventType = event.type();
    subscribers.stream().filter(subscriber -> subscriber.type().equals(eventType))
        .forEach(subscriber -> subscriber.onReceive(event));
  }
}
