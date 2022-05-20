package life.qbic.messaging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import life.qbic.apps.datamanager.notifications.MessageBusInterface;
import life.qbic.apps.datamanager.notifications.MessageParameters;
import life.qbic.apps.datamanager.notifications.MessageSubscriber;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class Exchange implements MessageBusInterface {

  List<Topic> topics;

  private static Exchange instance;

  public static Exchange instance() {
    if (instance == null) {
      instance = new Exchange();
    }
    return instance;
  }

  protected Exchange() {
    super();
    topics = new ArrayList<>();
  }

  @Override
  public void submit(String message, MessageParameters messageParameters) {
    this.topics.forEach(it -> it.informAllSubscribers(message, messageParameters));
  }

  synchronized public void subscribe(MessageSubscriber subscriber, String notificationType) {
    Topic matchingTopic = null;
    for (Topic availableTopic : topics) {
      if (availableTopic.matchesTopic(notificationType)) {
        matchingTopic = availableTopic;
        break;
      }
    }
    if (matchingTopic == null) {
      matchingTopic = new Topic(notificationType);
      topics.add(matchingTopic);
    }
    matchingTopic.addSubscriber(subscriber);

  }

  static class Topic {

    private final String topic;

    private final Set<MessageSubscriber> subscribers;

    static Topic create(String topic) {
      return new Topic(topic);
    }

    protected Topic(String topic) {
      super();
      this.topic = topic;
      subscribers = new HashSet<>();
    }

    synchronized void addSubscriber(MessageSubscriber subscriber) {
      subscribers.add(subscriber);
    }

    synchronized void removeSubscriber(MessageSubscriber subscriber) {
      subscribers.remove(subscriber);
    }

    boolean matchesTopic(String topic) {
      return this.topic.equalsIgnoreCase(topic);
    }

    synchronized void informAllSubscribers(String message, MessageParameters messageParameters) {
      if (messageParameters.messageType.equalsIgnoreCase(topic)) {
        informSubscribers(message, messageParameters);
      }
    }

    private void informSubscribers(String message, MessageParameters messageParameters) {
      subscribers.forEach(it -> it.receive(message, messageParameters));
    }
  }

}
