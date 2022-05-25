package life.qbic.messaging;

import java.util.Deque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import life.qbic.apps.datamanager.notifications.MessageBusInterface;
import life.qbic.apps.datamanager.notifications.MessageParameters;
import life.qbic.apps.datamanager.notifications.MessageSubscriber;

/**
 * <b>Exchange (messaging)</b>
 *
 * <p>The Exchange class is a simple implementation of the {@link MessageBusInterface} and can be
 * used to broadcast messages out of the data manager and user management context.
 *
 * <p>Note: use this class for development purposes only and replace it with an implementation that
 * utilizes a production grade messaging middleware, such as for example RabbitMQ or Apache Kafka.
 *
 * @since 1.0.0
 */
public class Exchange implements MessageBusInterface {

  private static final int DEFAULT_CAPACITY = 100;
  private final Queue<SubmissionTask> submissionTasks;

  private final Deque<Topic> topics;

  private static Exchange instance;

  private final int maxCapacity;

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

  /**
   * Queries an instance of the Exchange class. If none exists, one will be created with the
   * provided capacity for the messages the exchange instance can hold in its queue.
   * <p>
   * Note: if the instance already exists prior to this call, then the capacity argument will be
   * ignored.
   *
   * @param capacity the capacity of the queue size
   * @return an exchange instance
   * @since 1.0.0
   */
  public static Exchange instance(int capacity) {
    if (instance == null) {
      instance = new Exchange(capacity);
    }
    return instance;
  }

  protected Exchange(int capacity) {
    topics = new ConcurrentLinkedDeque<>();
    submissionTasks = new ArrayBlockingQueue<>(capacity);
    this.maxCapacity = capacity;
    launchSubmissionTaskWorker();
  }

  protected Exchange() {
    this(DEFAULT_CAPACITY);
  }

  private void launchSubmissionTaskWorker() {
    Thread worker = new SubmissionTaskWorker(this, topics);
    worker.setName("Message Submission Worker");
    worker.start();
  }

  /**
   * Submits a message to the exchange. The topic is taken from the
   * {@link MessageParameters#messageType} parameter, and all subscriber to this topic are
   * informed.
   *
   * @param message           the message to publish via the exchange instance
   * @param messageParameters some message parameters, such as the type (aka topic), the occuredOn
   *                          timepoint and a unique message identifier.
   */
  @Override
  synchronized public void submit(String message, MessageParameters messageParameters) {
    SubmissionTask newTask = new SubmissionTask(message, messageParameters);
    addSubmissionTask(newTask);
  }

  synchronized protected SubmissionTask getSubmissionTask() {
    while (submissionTasks.isEmpty()) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    SubmissionTask task = submissionTasks.remove();
    notifyAll();
    return task;
  }

  synchronized protected void addSubmissionTask(SubmissionTask task) {
    while (submissionTasks.size() == maxCapacity) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    submissionTasks.add(task);
    notifyAll();
  }

  /**
   * Subscribe to a topic in order to get informed, whenever a message with this topic is published
   * over this Exchange instance.
   *
   * @param subscriber the subscriber callback reference. A subscriber can only subscribe once to a
   *                   topic. Multiple calls will not overwrite the subscriber.
   * @param topic      the topic to subscribe to
   */
  @Override
  public void subscribe(MessageSubscriber subscriber, String topic) {
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

  /**
   * Small helper class to handle topics and their subscribers.
   */
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

  static class SubmissionTask {

    String message;

    MessageParameters messageParameters;

    SubmissionTask(String message, MessageParameters messageParameters) {
      this.message = message;
      this.messageParameters = messageParameters;
    }

  }

  static class SubmissionTaskWorker extends Thread {

    Exchange exchange;

    Deque<Topic> topics;

    SubmissionTaskWorker(Exchange exchange, Deque<Topic> topics) {
      this.exchange = exchange;
      this.topics = topics;
    }

    private void submit(String message, MessageParameters messageParameters) {
      this.topics.forEach(it -> it.informAllSubscribers(message, messageParameters));
    }

    @Override
    public void run() {
      while (true) {
        if (Thread.currentThread().isInterrupted()) {
          cleanup();
          return;
        }
        try {
          handleSubmissions();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void cleanup() {
      // do potential cleanup work when the worker receives
      // an interrupted signal
    }

    private void handleSubmissions() throws InterruptedException {
      var task = exchange.getSubmissionTask();
      if (task == null) {
        return;
      }
      submit(task.message, task.messageParameters);
    }

  }
}
