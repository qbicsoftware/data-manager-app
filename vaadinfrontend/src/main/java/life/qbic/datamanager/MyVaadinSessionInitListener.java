package life.qbic.datamanager;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.server.ServiceInitEvent;
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
        initEvent -> {
          log.info("A new Session has been initialized!");
        });

    event.getSource().addUIInitListener(
        initEvent -> {
          log.info("A new UI has been initialized!");
          UI ui = initEvent.getUI();
          ui.getPage().retrieveExtendedClientDetails(clientDetailsReceiver);
          ui.getSession().setErrorHandler(errorEvent -> uiExceptionHandler.error(errorEvent, ui));
        });
  }
}
