package life.qbic.messaging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
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

  private final List<Topic> topics;

  private static Exchange instance;

  private final ReentrantLock lock;

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

  protected Exchange(int capacity) {
    topics = new ArrayList<>();
    submissionTasks = new ArrayBlockingQueue<>(capacity);
    lock = new ReentrantLock();
    launchSubmissionTaskWorker();
  }

  protected Exchange() {
    this(DEFAULT_CAPACITY);
  }

  private void launchSubmissionTaskWorker() {
    Thread worker = new SubmissionTaskWorker(submissionTasks, topics, lock);
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
  public void submit(String message, MessageParameters messageParameters) {
    SubmissionTask newTask = new SubmissionTask(message, messageParameters);
    submissionTasks.add(newTask);
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
    try {
      // We try to acquire the lock for 1 second. If the lock
      // cannot be acquired within 1 second, we throw an exception
      // so the client can try to subscribe again.
      if (lock.tryLock(1000, TimeUnit.MILLISECONDS)) {
        try {
          tryToSubscribe(subscriber, topic);
        } finally {
          lock.unlock(); // release the lock afterwards
        }
      } else {
        throw new RuntimeException("Subscription failed");
      }
    } catch (InterruptedException e) {
      return; // no need to do anything
    }
  }

  private void tryToSubscribe(MessageSubscriber messageSubscriber, String topic) {
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
    matchingTopic.addSubscriber(messageSubscriber);
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

    void addSubscriber(MessageSubscriber subscriber) {
      subscribers.add(subscriber);
    }

    void removeSubscriber(MessageSubscriber subscriber) {
      subscribers.remove(subscriber);
    }

    boolean matchesTopic(String topic) {
      return this.topic.equalsIgnoreCase(topic);
    }

    void informAllSubscribers(String message, MessageParameters messageParameters) {
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

    Queue<SubmissionTask> tasks;

    List<Topic> topics;

    ReentrantLock lock;

    SubmissionTaskWorker(Queue<SubmissionTask> tasks, List<Topic> topics,
        ReentrantLock lock) {
      this.tasks = tasks;
      this.lock = lock;
      this.topics = topics;
    }

    private void submit(String message, MessageParameters messageParameters) {
      this.topics.forEach(it -> it.informAllSubscribers(message, messageParameters));
    }

    @Override
    public void run() {
      while (true) {
        if (Thread.currentThread().isInterrupted()) {
          return;
        }
        SubmissionTask currentTask = tasks.poll();
        if (currentTask != null) {
          try {
            handleSubmission(currentTask);
          } catch (InterruptedException ignored) {
            return;
          }
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignored) {
          // no need to do anything atm, we don't save the state of the working queue.
          // we return if the Thread gets interrupted, which ends the worker
          return;
        }
      }
    }

    private void handleSubmission(SubmissionTask currentTask) throws InterruptedException {
      while (!lock.tryLock()) {
        Thread.sleep(100);
      }
      try {
        submit(currentTask.message, currentTask.messageParameters);
      } finally {
        lock.unlock();
      }
    }

  }
}
