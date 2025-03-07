package life.qbic.datamanager.exceptionhandling.routing;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import java.util.Locale;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;

/**
 * <b>An error page displayed when an exception is thrown during routing</b>
 * <p>
 * Indicates the ability to display an error page for a specific type of exception. There can only
 * be one implementing class per exception class.
 * <p>
 * Vaadin redirects exceptions of type {@link E} to this class. The error page is responsible for
 * logging the exception and displaying an appropriate page.
 *
 * @param <E> the type of exception that is caught. If another ErrorPage<? extends E> exists it will
 *            be used instead.
 * @since 1.0.0
 */
public interface ErrorPage<E extends Exception> extends HasErrorParameter<E> {

  /**
   * By default the page will not be modified.
   * <p>
   * May be overwritten to show error specific information. This method may modify the UI to display
   * exception specific information.
   *
   * @param error the error for which to adapt the page.
   */
  default void showError(E error, Locale locale) {

  }

  /**
   * Logs the error. When overwriting this method, please ensure errors are logged.
   *
   * @param error the error to log.
   */
  default void logError(E error) {
    Logger log = logger(getClass());
    log.error(error.getMessage());
    log.debug(error.getMessage(), error);
  }

  /**
   * <b>Note!</b> returned int should be a valid Http status code
   * (see <a href="https://www.rfc-editor.org/rfc/rfc2068#section-6.1.1">rfc2068</a>)
   *
   * @return a matching HTTP status code
   */
  int getStatusCode();

  @Override
  default int setErrorParameter(BeforeEnterEvent event, ErrorParameter<E> parameter) {
    E exception = parameter.getException();
    logError(exception);
    showError(exception, event.getUI().getLocale());
    return getStatusCode();
  }
}
