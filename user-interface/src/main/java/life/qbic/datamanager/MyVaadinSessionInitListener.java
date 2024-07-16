package life.qbic.datamanager;

import static java.util.Objects.isNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import life.qbic.datamanager.exceptionhandling.UiExceptionHandler;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.register.RegistrationOrcIdMain;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Adds listeners to vaadin sessions and ui initialization.
 */
@SpringComponent
public class MyVaadinSessionInitListener implements VaadinServiceInitListener,
    SessionDestroyListener {

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
        initEvent -> log.debug("A new Session has been initialized! Session " + initEvent.getSession().getSession().getId()));

    event.getSource().addSessionDestroyListener(this);

    event.getSource().addUIInitListener(
        initEvent -> {
          log.debug("A new UI has been initialized! Session is " + initEvent.getUI().getSession().getSession().getId());
          UI ui = initEvent.getUI();
          ui.getPage().retrieveExtendedClientDetails(clientDetailsReceiver);
          ui.getSession().setErrorHandler(errorEvent -> uiExceptionHandler.error(errorEvent, ui));
          ui.addBeforeEnterListener(this::ensureCompleteOidcRegistration);
        });


  }

  @Override
  public void sessionDestroy(SessionDestroyEvent event) {
    log.debug("Session destroyed.");
    event.getSession().getSession().invalidate();
    log.debug("HTTP Session has been invalidated. Id is " + event.getSession().getSession().getId());
  }

  private void ensureCompleteOidcRegistration(BeforeEnterEvent it) {
    SecurityContext securityContext = SecurityContextHolder.getDeferredContext().get();
    Authentication authentication = securityContext.getAuthentication();
    if (isNull(authentication)) {
      return;
    }
    var principal = authentication.getPrincipal();
    if (principal instanceof OidcUser && !(principal instanceof QbicOidcUser)) {
      if (it.getNavigationTarget().equals(RegistrationOrcIdMain.class)) {
        return;
      }
      log.warn("Incomplete OpenIdConnect registration. Logging out and forwarding to login.");
      logoutService.logout();
      it.forwardTo(AppRoutes.LOGIN);
    }
  }
}
