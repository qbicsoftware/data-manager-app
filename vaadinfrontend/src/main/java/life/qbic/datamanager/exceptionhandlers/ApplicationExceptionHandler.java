package life.qbic.datamanager.exceptionhandlers;

import com.vaadin.flow.component.UI;
import life.qbic.application.commons.ApplicationException;

/**
 * Handles an application exception and presents it to the output.
 */
public interface ApplicationExceptionHandler {

  /**
   * Handles an application exception and presents it to the user. Does not log the exception as it
   * is assumed that the exception does not provide a complete stacktrace.
   *
   * @param ui        the UI in which the exception should be presented
   * @param exception the exception
   * @since 1.0.0
   */
  void handle(UI ui, ApplicationException exception);
}
