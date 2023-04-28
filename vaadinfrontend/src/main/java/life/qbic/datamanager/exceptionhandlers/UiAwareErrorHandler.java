package life.qbic.datamanager.exceptionhandlers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;

public interface UiAwareErrorHandler {


  /**
   * Invoked when an error occurs.
   *
   * @param event the fired event.
   * @param ui    the ui in which the error event is fired
   */
  void error(ErrorEvent event, UI ui);
}
