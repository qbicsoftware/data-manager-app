package life.qbic.logging.api;

/**
 * Simplistic interface of a {@link Logger}, that enables basic logging functionality for different
 * log levels.
 *
 * @since 1.0.0
 */
public interface Logger {

  /**
   * Logs a message on DEBUG level
   *
   * @param message the message content
   * @since 1.0.0
   */
  void debug(String message);

  /**
   * Logs a message on DEBUG level
   *
   * @param message the message content
   * @param cause   the cause of the finding to report
   * @since 1.0.0
   */
  void debug(String message, Throwable cause);

  /**
   * Logs a message on ERROR level
   *
   * @param message the message content
   * @since 1.0.0
   */
  void error(String message);

  /**
   * Logs a message on ERROR level with a {@link Throwable} provided
   *
   * @param message the message content
   * @param cause   the throwable that represents the cause
   * @since 1.0.0
   */
  void error(String message, Throwable cause);

  /**
   * Logs a message on INFO level
   *
   * @param message the message content
   * @since 1.0.0
   */
  void info(String message);

  /**
   * Logs a message on WARNING level
   *
   * @param message the message content
   * @since 1.0.0
   */
  void warn(String message);

}
