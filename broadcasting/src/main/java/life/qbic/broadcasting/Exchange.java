package life.qbic.broadcasting;

import java.util.Deque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * <b>Exchange (messaging)</b>
 *
 * <p>The Exchange class is a simple implementation of the {@link MessageBusSubmission} and can be
 * used to broadcast messages out of the data manager and user management context.
 *
 * <p>Note: use this class for development purposes only and replace it with an implementation that
 * utilizes a production grade messaging middleware, such as for example RabbitMQ or Apache Kafka.
 *
 * @since 1.0.0
 */
public class Exchange implements MessageBusSubmission, MessageSubscription {

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
    worker.setName("DisplayMessage Submission Worker");
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
  public synchronized void submit(String message, MessageParameters messageParameters) {
    SubmissionTask newTask = new SubmissionTask(message, messageParameters);
    addSubmissionTask(newTask);
  }

  /**
   * Queries the next available {@link SubmissionTask} from the {@link Exchange}.
   * <p>
   * Note: this method is not guaranteed to return promptly.
   * <p>
   * This method will return only at once a submission task is available. If no task is available,
   * the client thread will go into the {@link Object#wait()} state. The exchange will notify all
   * waiting threads once a new submission task is available.
   *
   * @return a submission task
   * @since 1.0.0
   */
  protected synchronized SubmissionTask getSubmissionTask() {
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

  /**
   * Adds a new submission task to the exchange.
   * <p>
   * Note: this method is not guaranteed to return immediately. If the exchange has reached its
   * current maximum capacity, the client thread will be suspended into the {@link Object#wait()}
   * state.
   * <p>
   * Once the exchange has free capacity again, the method will return.
   *
   * @param task a new submission task to be added to exchange
   * @since 1.0.0
   */
  protected synchronized void addSubmissionTask(SubmissionTask task) {
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
   * Subscribe to a topic in order to be informed, whenever a message with this topic is published
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

    private final String value;

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
      this.value = topic;
      subscribers = new HashSet<>();
    }

    synchronized void addSubscriber(MessageSubscriber subscriber) {
      subscribers.add(subscriber);
    }

    synchronized void removeSubscriber(MessageSubscriber subscriber) {
      subscribers.remove(subscriber);
    }

    boolean matchesTopic(String topic) {
      return this.value.equalsIgnoreCase(topic);
    }

    synchronized void informAllSubscribers(String message, MessageParameters messageParameters) {
      if (messageParameters.messageType.equalsIgnoreCase(value)) {
        informSubscribers(message, messageParameters);
      }
    }

    private void informSubscribers(String message, MessageParameters messageParameters) {
      subscribers.forEach(it -> it.receive(message, messageParameters));
    }
  }

  /**
   * A submission task contains the original message and its message parameters
   * ({@link MessageParameters}).
   *
   * @since 1.0.0
   */
  static class SubmissionTask {

    String message;

    MessageParameters messageParameters;

    SubmissionTask(String message, MessageParameters messageParameters) {
      this.message = message;
      this.messageParameters = messageParameters;
    }

  }

  /**
   * Small helper class that can be used to listen to an {@link Exchange} instance and process
   * incoming new {@link SubmissionTask}s.
   *
   * @since 1.0.0
   */
  static class SubmissionTaskWorker extends Thread {

    Exchange exchange;

    Deque<Topic> topics;

    /**
     * This registers an instance of the {@link Exchange} class to a submission worker.
     *
     * @param exchange an instance of the {@link} Exchange class, from which submission tasks will
     *                 be consumed.
     * @param topics   a linear collection of topics. The worker processes a {@link SubmissionTask}
     *                 and tries to find the corresponding topic. If a task matches a topic, all
     *                 subscribers are informed.
     * @since 1.0.0
     */
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
      // Beware that this call is blocking. If no new submission tasks are available,
      // the worker is going into the Object.wait() state.
      var task = exchange.getSubmissionTask();
      if (task == null) {
        return;
      }
      submit(task.message, task.messageParameters);
    }

  }
}
