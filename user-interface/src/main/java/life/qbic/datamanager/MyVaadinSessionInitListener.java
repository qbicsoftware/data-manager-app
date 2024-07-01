package life.qbic.datamanager;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.datamanager.exceptionhandling.UiExceptionHandler;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.register.RegisterOpenIdConnect;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Adds listeners to vaadin sessions and ui initialization.
 */
@SpringComponent
public class MyVaadinSessionInitListener implements VaadinServiceInitListener {

  private static final Logger log = logger(MyVaadinSessionInitListener.class);
  private final ExtendedClientDetailsReceiver clientDetailsReceiver;

  private final UiExceptionHandler uiExceptionHandler;
  private final LogoutService logoutService;

  public MyVaadinSessionInitListener(
      @Autowired ExtendedClientDetailsReceiver clientDetailsProvider,
      @Autowired UiExceptionHandler uiExceptionHandler,
      LogoutService logoutService) {
    this.clientDetailsReceiver = clientDetailsProvider;
    this.uiExceptionHandler = uiExceptionHandler;
    this.logoutService = logoutService;
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.getSource().addSessionInitListener(
        initEvent -> log.info("A new Session has been initialized!"));

    event.getSource().addUIInitListener(
        initEvent -> {
          log.info("A new UI has been initialized!");
          UI ui = initEvent.getUI();
          ui.getPage().retrieveExtendedClientDetails(clientDetailsReceiver);
          ui.getSession().setErrorHandler(errorEvent -> uiExceptionHandler.error(errorEvent, ui));
          ui.addBeforeEnterListener(this::ensureCompleteOidcRegistration);
        });
  }

  private void ensureCompleteOidcRegistration(BeforeEnterEvent it) {
    SecurityContext securityContext = SecurityContextHolder.getDeferredContext().get();
    var principal = securityContext.getAuthentication().getPrincipal();
    if (principal instanceof OidcUser && !(principal instanceof QbicOidcUser)) {
      if (it.getNavigationTarget().equals(RegisterOpenIdConnect.class)) {
        return;
      }
      log.warn("Incomplete OpenIdConnect registration. Logging out and forwarding to login.");
      logoutService.logout();
      it.forwardTo(AppRoutes.LOGIN);
    }
  }
}
