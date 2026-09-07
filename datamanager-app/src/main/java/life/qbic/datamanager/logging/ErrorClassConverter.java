package life.qbic.datamanager.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;

/**
 * Logback converter that renders only the class name of the logged throwable.
 * <p>
 * It is used by the error-notification e-mail appender so that no log message content or stack
 * trace is transmitted (data protection / DSGVO data minimisation). A developer sees only the
 * time and the error class and must consult the on-premises log file for details.
 * <p>
 * Extending {@link ThrowableProxyConverter} ensures Logback's automatic exception handling
 * treats the throwable as "handled" and does not append the full stack trace.
 *
 * @since 1.0.0
 */
public class ErrorClassConverter extends ThrowableProxyConverter {

  @Override
  protected String throwableProxyToString(IThrowableProxy throwableProxy) {
    if (throwableProxy == null) {
      return "";
    }
    return throwableProxy.getClassName();
  }
}