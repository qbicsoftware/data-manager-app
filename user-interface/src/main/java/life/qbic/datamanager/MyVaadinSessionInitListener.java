package life.qbic.datamanager;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.datamanager.exceptionhandling.UiExceptionHandler;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adds listeners to vaadin sessions and ui initialization.
 */
@SpringComponent
public class MyVaadinSessionInitListener implements VaadinServiceInitListener,
    SessionDestroyListener {

  private static final Logger log = logger(MyVaadinSessionInitListener.class);
  private final ExtendedClientDetailsReceiver clientDetailsReceiver;

  private final UiExceptionHandler uiExceptionHandler;

  public MyVaadinSessionInitListener(
      @Autowired ExtendedClientDetailsReceiver clientDetailsProvider,
      @Autowired UiExceptionHandler uiExceptionHandler) {
    this.clientDetailsReceiver = clientDetailsProvider;
    this.uiExceptionHandler = uiExceptionHandler;
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.getSource().addSessionInitListener(
        initEvent -> log.debug("A new Session has been initialized! Session " + initEvent.getSession().getSession().getId()));

    event.getSource().addSessionDestroyListener(this);

    event.getSource().addUIInitListener(
        initEvent -> {
          log.debug("A new UI has been initialized! Session is " + initEvent.getUI().getSession().getSession().getId());
          UI ui = initEvent.getUI();
          ui.getPage().retrieveExtendedClientDetails(clientDetailsReceiver);
          ui.getSession().setErrorHandler(errorEvent -> uiExceptionHandler.error(errorEvent, ui));
        });


  }

  @Override
  public void sessionDestroy(SessionDestroyEvent event) {
    log.debug("Session destroyed.");
    event.getSession().getSession().invalidate();
    log.debug("HTTP Session has been invalidated. Id is " + event.getSession().getSession().getId());
  }
}
