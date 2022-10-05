package life.qbic.datamanager;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Adds listeners to vaadin sessions and ui initialization.
 */
@SpringComponent
public class MyVaadinSessionInitListener implements VaadinServiceInitListener {

  private static final Logger log = logger(MyVaadinSessionInitListener.class);
  private final ExtendedClientDetailsReceiver clientDetailsReceiver;

  public MyVaadinSessionInitListener(
      @Autowired ExtendedClientDetailsReceiver clientDetailsProvider) {
    this.clientDetailsReceiver = clientDetailsProvider;
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.getSource().addSessionInitListener(
        initEvent -> log.info("A new Session has been initialized!"));

    event.getSource().addUIInitListener(
        initEvent -> {
          log.info("A new UI has been initialized!");
          initEvent.getUI().getPage().retrieveExtendedClientDetails(clientDetailsReceiver);
        });
  }
}
