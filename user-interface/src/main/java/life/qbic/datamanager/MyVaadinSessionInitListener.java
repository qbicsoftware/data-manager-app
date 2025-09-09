package life.qbic.datamanager;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page.ExtendedClientDetailsReceiver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.security.AuthenticationContext;
import life.qbic.datamanager.exceptionhandling.UiExceptionHandler;
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

  private final AuthenticationContext authenticationContext;
  private final transient UiExceptionHandler uiExceptionHandler;

  public MyVaadinSessionInitListener(
      @Autowired ExtendedClientDetailsReceiver clientDetailsProvider,
      @Autowired UiExceptionHandler uiExceptionHandler,
      @Autowired AuthenticationContext authenticationContext) {
    this.clientDetailsReceiver = clientDetailsProvider;
    this.uiExceptionHandler = uiExceptionHandler;
    this.authenticationContext = requireNonNull(authenticationContext);
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
      requireNonNull(authenticationContext).logout();
    }
  }
}
