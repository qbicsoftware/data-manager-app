package life.qbic.messaging;

import life.qbic.apps.datamanager.notifications.MessageBusInterface;
import life.qbic.apps.datamanager.notifications.MessageParameters;
import life.qbic.apps.datamanager.notifications.MessageSubscriber;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <b>Exchange (messaging)</b>
 *
 * <p>The Exchange class is a simple implementation of the {@link MessageBusInterface} and can be
 * used to broadcast messages out of the data manager and user management context.
 *
 * <p>Note: use this class for development purposes only and replace it with an implementation that
 * utilizes a production grade messaging middleware, such as for example RabbitMQ or Apache Kafka.
 *
 * @since <version tag>
 */
public class Exchange implements MessageBusInterface {

  List<Topic> topics;

  private static Exchange instance;

  /**
   * Queries the current instance of the Exchange class.
   *
   * @return 1.0.0
   */
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

  /**
   * Submits a message to the exchange. The topic is taken from the {@link
   * MessageParameters#messageType} parameter, and all subscriber to this topic are informed.
   *
   * @param message the message to publish via the exchange instance
   * @param messageParameters some message parameters, such as the type (aka topic), the occuredOn
   *     timepoint and a unique message identifier.
   */
  @Override
  public synchronized void submit(String message, MessageParameters messageParameters) {
    this.topics.forEach(it -> it.informAllSubscribers(message, messageParameters));
  }

  /**
   * Subscribe to a topic in order to get informed, whenever a message with this topic is published
   * over this Exchange instance.
   *
   * @param subscriber the subscriber callback reference. A subscriber can only subscribe once to a
   *     topic. Multiple calls will not overwrite the subscriber.
   * @param topic the topic to subscribe to
   */
  @Override
  public synchronized void subscribe(MessageSubscriber subscriber, String topic) {
    Topic matchingTopic = null;
    for (Topic availableTopic : topics) {
      if (availableTopic.matchesTopic(topic)) {
        matchingTopic = availableTopic;
        break;
      }
    }
    if (matchingTopic == null) {
      matchingTopic = new Topic(topic);
      topics.add(matchingTopic);
    }
    matchingTopic.addSubscriber(subscriber);
  }

  /** Small helper class to handle topics and their subscribers. */
  static class Topic {

    private final String topic;

    private final Set<MessageSubscriber> subscribers;

    /**
     * Creates a new topic.
     *
     * @param topic the new message topic that clients can subscribe to
     * @return a topic
     */
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
