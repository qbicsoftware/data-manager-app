package life.qbic.datamanager.views.register;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationComponent.UserRegistrationInformation;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserExistsException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * User Registration Main
 * <p>
 * {@link Main} component hosting the {@link UserRegistrationComponent} responsible for handling the
 * trigger of a new user registration request.
 */
@PageTitle("Register")
@Route(value = AppRoutes.REGISTER, layout = LandingPageLayout.class)
@AnonymousAllowed
@SpringComponent
@UIScope
public class UserRegistrationMain extends Main {

  @Serial
  private static final long serialVersionUID = 6995209728843801219L;

  private static final Logger log =
      LoggerFactory.logger(UserRegistrationMain.class.getName());
  private final transient IdentityService identityService;
  private final transient UserInformationService userInformationService;
  private final UserRegistrationComponent userRegistrationComponent;

  public UserRegistrationMain(@Autowired IdentityService identityService, @Autowired
  UserInformationService userInformationService) {
    this.identityService = Objects.requireNonNull(identityService,
        "Identity service cannot be null");
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "User Information service cannot be null");
    addClassName("user-registration");
    userRegistrationComponent = new UserRegistrationComponent(userInformationService);
    userRegistrationComponent.addRegistrationListener(
        event -> onRegistrationClicked(event.userRegistrationInformation()));
    add(userRegistrationComponent);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        userRegistrationComponent.getClass().getSimpleName(),
        System.identityHashCode(userRegistrationComponent)));
  }

  private void onRegistrationClicked(UserRegistrationInformation userRegistrationInformation) {
    identityService.registerUser(
            userRegistrationInformation.fullName(),
            userRegistrationInformation.userName(),
            userRegistrationInformation.email(),
            userRegistrationInformation.password().toCharArray())
        .ifSuccessOrElse(applicationResponse -> {
              var userId = userInformationService.findByEmail(userRegistrationInformation.email())
                  .orElseThrow().id();
              getUI().orElseThrow().navigate(EmailConfirmationMain.class, userId);
            },
            applicationResponse -> handleRegistrationFailure(applicationResponse.failures()));
  }

  private void handleRegistrationFailure(List<RuntimeException> exceptionList) {
    /*These Cases should not happen anymore since we validate before we send the event,
    however they can still be used as a failsafe*/
    if (exceptionList.isEmpty()) {
      return;
    }
    for (RuntimeException e : exceptionList) {
      if (e instanceof UserExistsException) {
        userRegistrationComponent.showError("Email address already in use",
            "If you have difficulties with your password you can reset it.");
        break;
      } else if (e instanceof UserNameNotAvailableException) {
        userRegistrationComponent.showError("Username already in use",
            "Please try another username");
        break;
      } else if (e instanceof EmptyUserNameException) {
        userRegistrationComponent.showError("Username must not be empty",
            "Please try another username");
        break;
      } else {
        userRegistrationComponent.showError("Registration failed", "Please try again.");
        break;
      }
    }
    String allErrorMessages = exceptionList.stream().map(Throwable::getMessage)
        .collect(Collectors.joining("\n"));
    log.error(allErrorMessages);
    exceptionList.forEach(e -> log.debug(e.getMessage(), e));
  }
}
