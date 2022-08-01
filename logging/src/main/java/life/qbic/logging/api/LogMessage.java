package life.qbic.logging.api;

/**
 * Simple logging message record that holds basic information like:
 * <ul>
 *   <li>application</li>
 *   <li>log level</li>
 *   <li>message</li>
 *   <li>throwable</li>
 * </ul>
 *
 * @since 1.0.0
 */
public record LogMessage(String application, LogLevel logLevel, String message,
                         Throwable throwable) {

}
