package life.qbic.logging.subscription.api;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Describes a simple {@link Subscriber} that can be informed via its public method
 * {@link Subscriber#onMessageArrived(LogMessage)}.
 *
 * @since 1.0.0
 */
public interface Subscriber {

  static List<Subscriber> subscribers() {
    ServiceLoader<Subscriber> services = ServiceLoader.load(Subscriber.class);
    List<Subscriber> list = new ArrayList<>();
    services.iterator().forEachRemaining(list::add);
    return list;
  }

  /**
   * Informs the subscriber about a new incoming {@link LogMessage}.
   *
   * @param logMessage the log message that was broadcast
   * @since 1.0.0
   */
  void onMessageArrived(LogMessage logMessage);

}
