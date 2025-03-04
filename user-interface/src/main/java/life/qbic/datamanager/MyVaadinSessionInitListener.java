package life.qbic.datamanager;

import static java.util.Objects.isNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.io.IOException;
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
public class MyVaadinSessionInitListener implements VaadinServiceInitListener {

  private static final Logger log = logger(MyVaadinSessionInitListener.class);
  private final ExtendedClientDetailsReceiver clientDetailsReceiver;

  private final transient UiExceptionHandler uiExceptionHandler;
  private final LogoutService logoutService;

  public MyVaadinSessionInitListener(
      @Autowired ExtendedClientDetailsReceiver clientDetailsProvider,
      @Autowired UiExceptionHandler uiExceptionHandler,
      @Autowired LogoutService logoutService) {
    this.clientDetailsReceiver = clientDetailsProvider;
    this.uiExceptionHandler = uiExceptionHandler;
    this.logoutService = logoutService;
  }

  @Override
  public void serviceInit(ServiceInitEvent event) {
    event.getSource().addSessionInitListener(MyVaadinSessionInitListener::onSessionInit);
    event.getSource().addServiceDestroyListener(MyVaadinSessionInitListener::onServiceDestroyed);
    event.getSource().addSessionDestroyListener(MyVaadinSessionInitListener::onSessionDestroy);
    event.getSource().addUIInitListener(this::onUiInit);
  }

  private void onUiInit(UIInitEvent initEvent) {
    log.debug("A new UI has been initialized! ui[%s] vaadin[%s] http[%s] ".formatted(
        initEvent.getUI().getUIId(), initEvent.getUI().getSession().getPushId(),
        initEvent.getUI().getSession().getSession().getId()));
    UI ui = initEvent.getUI();
    ui.getPage().retrieveExtendedClientDetails(clientDetailsReceiver);
    ui.getSession().setErrorHandler(errorEvent -> uiExceptionHandler.error(errorEvent, ui));
    ui.addBeforeEnterListener(this::ensureCompleteOidcRegistration);
  }

  private static void onSessionInit(SessionInitEvent initEvent) {
    log.debug("A new Session has been initialized! vaadin[%s] http[%s] ".formatted(
        initEvent.getSession().getPushId(), initEvent.getSession().getSession().getId()));
  }

  private static void onServiceDestroyed(ServiceDestroyEvent serviceDestroyEvent) {
    log.debug("Destroying vaadin service [%s]".formatted(serviceDestroyEvent.getSource()));
  }

  public static void onSessionDestroy(SessionDestroyEvent event) {
    WrappedSession wrappedSession = event.getSession().getSession();
    if (wrappedSession != null) {
      wrappedSession.invalidate();
      log.debug("Invalidated HTTP session " + wrappedSession.getId());
    } else {
      log.debug("Vaadin session [%s] does not wrap any HTTP session.".formatted(
          event.getSession().getPushId()));
    }
    log.debug("Vaadin session destroyed [%s].".formatted(event.getSession().getPushId()));

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
