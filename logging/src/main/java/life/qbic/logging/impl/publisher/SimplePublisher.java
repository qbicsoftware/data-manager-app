package life.qbic.logging.impl.publisher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import life.qbic.logging.api.Publisher;
import life.qbic.logging.subscription.api.LogMessage;
import life.qbic.logging.subscription.api.Subscriber;

/**
 * Basic implementation of the {@link Publisher} interface.
 *
 * @since 1.0.0
 */
class SimplePublisher implements Publisher {

  private static final int DEFAULT_MESSAGE_CAPACITY = 100;

  private final Collection<Subscriber> subscribers;

  private final Queue<LogMessage> logMessages;

  public SimplePublisher() {
    subscribers = Collections.synchronizedCollection(new HashSet<>());
    logMessages = new ArrayBlockingQueue<>(DEFAULT_MESSAGE_CAPACITY);
    Broadcasting broadcasting = new Broadcasting(subscribers, this);
    broadcasting.start();
  }

  @Override
  public void subscribe(Subscriber s) {
    requireNonNull(s, "Subscriber must not be null");
    synchronized (subscribers) {
      subscribers.add(s);
    }
  }

  @Override
  public void unsubscribe(Subscriber s) {
    requireNonNull(s, "Subscriber must not be null");
    synchronized (subscribers) {
      subscribers.remove(s);
    }
  }

  @Override
  public synchronized void publish(LogMessage logMessage) {
    requireNonNull(logMessage, "LogMessage must not be null");
    logMessages.add(logMessage);
    notifyAll();
  }

  protected synchronized LogMessage nextLogMessage() {
    while (logMessages.isEmpty()) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return logMessages.poll();
  }

  /**
   * Helper class for broadcasting the actual log messages in an own thread to ensure a non-blocking
   * message publication on the client side.
   *
   * @since 1.0.0
   */
  static class Broadcasting extends Thread {

    private final Collection<Subscriber> subscribers;
    private final SimplePublisher publisher;

    Broadcasting(Collection<Subscriber> subscribers, SimplePublisher publisher) {
      this.subscribers = subscribers;
      this.publisher = publisher;
    }

    @Override
    public void run() {
      while (true) {
        if (Thread.interrupted()) {
          cleanup();
          return;
        }
        try {
          handleBroadcasting();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    private void cleanup() {
      //Potential clean up tasks
    }

    private void handleBroadcasting() throws InterruptedException {
      var logMessage = publisher.nextLogMessage();
      if (isNull(logMessage)) {
        return;
      }
      submit(logMessage);
    }

    private void submit(LogMessage logMessage) {
      synchronized (subscribers) {
        subscribers.forEach(s -> s.onMessageArrived(logMessage));
      }
    }
  }

}
