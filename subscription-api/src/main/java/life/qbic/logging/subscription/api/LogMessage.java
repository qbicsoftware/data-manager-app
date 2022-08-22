package life.qbic.logging.subscription.api;

/**
 * Simple logging message record that holds basic information like:
 * <ul>
 *   <li>application</li>
 *   <li>log level</li>
 *   <li>message</li>
 *   <li>cause</li>
 * </ul>
 *
 * @since 1.0.0
 */
public record LogMessage(String application, LogLevel logLevel, String message,
                         Throwable cause) {

}
