package life.qbic.datamanager;

import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import static life.qbic.logging.service.LoggerFactory.logger;

/**
 * Adds listeners to vaadin sessions and ui initialization.
 */
@SpringComponent
public class MyVaadinSessionInitListener implements VaadinServiceInitListener {

  private static final Logger log = logger(MyVaadinSessionInitListener.class);
  private final ExtendedClientDetailsReceiver clientDetailsReceiver;

  private final ErrorHandler errorHandler;

  public MyVaadinSessionInitListener(
      @Autowired ExtendedClientDetailsReceiver clientDetailsProvider,
      @Autowired ErrorHandler errorHandler) {
    this.clientDetailsReceiver = clientDetailsProvider;
    this.errorHandler = errorHandler;
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.getSource().addSessionInitListener(
        initEvent -> {
          log.info("A new Session has been initialized!");
          initEvent.getSession().setErrorHandler(errorHandler);
        });

    event.getSource().addUIInitListener(
        initEvent -> {
          log.info("A new UI has been initialized!");
          initEvent.getUI().getPage().retrieveExtendedClientDetails(clientDetailsReceiver);
        });
  }
}
