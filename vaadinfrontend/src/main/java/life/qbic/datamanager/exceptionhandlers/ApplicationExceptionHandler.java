package life.qbic.datamanager.exceptionhandlers;

import com.vaadin.flow.component.UI;
import life.qbic.application.commons.ApplicationException;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface ApplicationExceptionHandler {

  void handle(UI ui, ApplicationException exception);
}
