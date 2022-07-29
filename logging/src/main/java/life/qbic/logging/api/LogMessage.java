package life.qbic.logging.api;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public record LogMessage(String application, LogLevel logLevel, String message, Throwable throwable) {}
