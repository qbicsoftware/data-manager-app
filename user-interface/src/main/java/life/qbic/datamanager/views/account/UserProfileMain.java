package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.domain.model.UserId;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * User Profile Main
 * <p>
 * This component hosts the components necessary to show the profile for the current logged-in User
 * via the {@link UserProfileComponent}. Additionally, the user can see the information within his
 * {@link life.qbic.identity.api.UserInfo} and change his Username via the provided UI elements
 */

@Route(value = "profile", layout = UserMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class UserProfileMain extends Main implements BeforeEnterObserver, AfterNavigationObserver {

  @Serial
  private static final long serialVersionUID = -5203169099713671493L;

  private final transient UserInformationService userInformationService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final IdentityService identityService;
  private final transient List<ParameterProcessor> parameterProcessors = new ArrayList<>();
  private final transient MessageSourceNotificationFactory messageFactory;


  public UserProfileMain(
      @Autowired UserInformationService userInformationService,
      @Autowired MessageSourceNotificationFactory messageFactory,
      AuthenticationToUserIdTranslationService userIdTranslator,
      IdentityService identityService) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");
    this.messageFactory = requireNonNull(messageFactory);
    addClassName("user-profile");
    this.identityService = identityService;
    parameterProcessors.add(this::processSuccessParam);
    parameterProcessors.add(this::processErrorParam);
  }

  /**
   * Upon initialization of the main Component, the {@link UserProfileComponent} should be provided
   * with the {@link UserId} of the currently logged-in user
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();
    var userInfo = userInformationService.findById(userId).orElseThrow();
    var profileComponent = new UserProfileComponent(identityService, userInfo, event.getLocation());
    add(profileComponent);
  }

  private void processRequestParams(QueryParameters parameters) {
    requireNonNull(parameters);
    parameterProcessors.forEach(p -> p.process(parameters));
  }

  private void processSuccessParam(QueryParameters routeParameters) {
    routeParameters.getSingleParameter("success").ifPresent(ignored -> {
      getUI().ifPresent(ui -> ui.access(() -> {
        messageFactory.toast("profile.oidc.link.success", new Object[]{}, getLocale()).open();
      }));
    });
  }

  private void processErrorParam(QueryParameters routeParameters) {
    routeParameters.getSingleParameter("error").ifPresent(reason -> {
      getUI().ifPresent(ui -> ui.access(() -> {
        messageFactory.toast("profile.oidc.link.failed", new Object[]{reason}, getLocale()).open();
      }));
    });
  }

  @Override
  public void afterNavigation(AfterNavigationEvent event) {
    processRequestParams(event.getLocation().getQueryParameters());
  }

  @FunctionalInterface
  interface ParameterProcessor {

    void process(QueryParameters routeParameters);

  }

}
