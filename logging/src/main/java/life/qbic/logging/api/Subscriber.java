package life.qbic.logging.api;

/**
 * Describes a simple {@link Subscriber} that can be informed via its public method
 * {@link Subscriber#onMessageArrived(LogMessage)}.
 *
 * @since 1.0.0
 */
public interface Subscriber {

  /**
   * Informs the subscriber about a new incoming {@link LogMessage}.
   *
   * @param logMessage the log message that was broadcast
   * @since 1.0.0
   */
  void onMessageArrived(LogMessage logMessage);

}
