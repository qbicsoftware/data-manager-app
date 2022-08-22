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

  /**
   * Returns a list of available subscriber providers.
   * <p>
   * All available providers of type {@link java.util.ServiceLoader.Provider} for the
   * {@link Subscriber} interface available in the class path are loaded via Java's
   * {@link ServiceLoader} mechanism.
   *
   * @return a list of available subscriber providers
   * @since 1.0.0
   */
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
