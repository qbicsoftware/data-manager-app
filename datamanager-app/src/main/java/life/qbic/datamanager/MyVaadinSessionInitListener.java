package life.qbic.datamanager;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.datamanager.exceptionhandling.UiExceptionHandler;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adds listeners to vaadin sessions and ui initialization.
 */
@SpringComponent
public class MyVaadinSessionInitListener implements VaadinServiceInitListener {

  private static final Logger log = logger(MyVaadinSessionInitListener.class);

  private final transient UiExceptionHandler uiExceptionHandler;

  public MyVaadinSessionInitListener(@Autowired UiExceptionHandler uiExceptionHandler) {
    this.uiExceptionHandler = requireNonNull(uiExceptionHandler,
        "uiExceptionHandler must not be null");
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.getSource().addSessionInitListener(MyVaadinSessionInitListener::onSessionInit);
    event.getSource().addServiceDestroyListener(MyVaadinSessionInitListener::onServiceDestroyed);
    event.getSource().addUIInitListener(this::onUiInit);
  }

  private void onUiInit(UIInitEvent initEvent) {
    log.debug("A new UI has been initialized! ui[%s] vaadin[%s] http[%s] ".formatted(
        initEvent.getUI().getUIId(), initEvent.getUI().getSession().getPushId(),
        initEvent.getUI().getSession().getSession().getId()));
    UI ui = initEvent.getUI();
    ui.getSession().setErrorHandler(errorEvent -> uiExceptionHandler.error(errorEvent, ui));
  }

  private static void onSessionInit(SessionInitEvent initEvent) {
    log.debug("A new Session has been initialized! vaadin[%s] http[%s] ".formatted(
        initEvent.getSession().getPushId(), initEvent.getSession().getSession().getId()));
  }

  private static void onServiceDestroyed(ServiceDestroyEvent serviceDestroyEvent) {
    log.debug("Destroying vaadin service [%s]".formatted(serviceDestroyEvent.getSource()));
  }

}
