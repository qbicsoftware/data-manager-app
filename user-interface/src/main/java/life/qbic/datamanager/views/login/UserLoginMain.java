package life.qbic.datamanager.views.login;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserExistsException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.identity.application.user.UserNotFoundException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * User Login Main
 * <p>
 * {@link Main} component hosting the {@link UserLoginComponent} responsible for handling the login
 * of a user into the application
 */
@PageTitle("Login")
@Route(value = AppRoutes.LOGIN, layout = LandingPageLayout.class)
@AnonymousAllowed
@SpringComponent
@UIScope
public class UserLoginMain extends Main implements HasUrlParameter<String> {

  @Serial
  private static final long serialVersionUID = -5505919914457512076L;

  private static final Logger log =
      LoggerFactory.logger(UserLoginMain.class.getName());
  private final transient IdentityService identityService;
  private final UserLoginComponent userLoginComponent;
  @Value("${EMAIL_CONFIRMATION_PARAMETER:confirm-email}")
  String emailConfirmationParameter;

  public UserLoginMain(
      @Value("${EMAIL_CONFIRMATION_PARAMETER:confirm-email}") String emailConfirmationParameter,
      @Autowired IdentityService identityService,
      @Autowired UserInformationService userInformationService) {
    this.identityService = Objects.requireNonNull(identityService,
        "Identity service cannot be null");
    this.emailConfirmationParameter = Objects.requireNonNull(emailConfirmationParameter);
    addClassName("user-login");
    userLoginComponent = new UserLoginComponent(userInformationService);
    add(userLoginComponent);
    addListener();
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        userLoginComponent.getClass().getSimpleName(),
        System.identityHashCode(userLoginComponent)));
  }


  private void handleLoginFailure(List<RuntimeException> exceptionList) {
    /*These Cases should not happen anymore since we validate before we send the event,
    however they can still be used as a failsafe*/
    if (exceptionList.isEmpty()) {
      return;
    }
    if (exceptionList.contains(UserExistsException.class)) {
      userLoginComponent.showError("Email address already in use",
          "If you have difficulties with your password you can reset it.");
    } else if (exceptionList.contains(UserNameNotAvailableException.class)) {
      userLoginComponent.showError("Username already in use", "Please try another username");
    } else if (exceptionList.contains(EmptyUserNameException.class)) {
      userLoginComponent.showError("Username must not be empty",
          "Please try another username");
    } else {
      userLoginComponent.showError("Registration failed", "Please try again.");
    }
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
    handle(event);
  }

  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> queryParams = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey("error")) {
      userLoginComponent.showInvalidCredentialsError();
    }
    if (queryParams.containsKey(emailConfirmationParameter)) {
      String userId = queryParams.get(emailConfirmationParameter).iterator().next();
      try {
        identityService.confirmUserEmail(userId);
        userLoginComponent.showEmailConfirmationInformation();
      } catch (UserNotFoundException e) {
        log.error("User %s not found!".formatted(userId), e);
        userLoginComponent.onEmailConfirmationFailure(
        );
      }

    }
    if (queryParams.containsKey("userRegistered")) {
      userLoginComponent.showEmailConfirmationReminder();
    }
  }

  private void addListener() {
    userLoginComponent.addLoginListener(it ->
        onLoginSucceeded());
    userLoginComponent.addForgotPasswordListener(
        it -> it.getSource().getUI().ifPresent(ui -> ui.navigate(AppRoutes.RESET_PASSWORD)));
  }

  private void onLoginSucceeded() {
    getUI().ifPresentOrElse(
        ui -> ui.navigate(Projects.PROJECTS),
        () -> log.error("No UI found!"));
  }
}
