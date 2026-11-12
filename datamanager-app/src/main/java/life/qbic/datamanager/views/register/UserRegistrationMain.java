package life.qbic.datamanager.views.register;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationComponent.UserRegistrationInformation;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserNames;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.identity.application.user.IdentityService.UserNameTooLongException;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
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
  private final transient UserInformationService userInformationService;
  private final transient RegistrationSubmission registrationSubmission;
  private final UserRegistrationComponent userRegistrationComponent;

  public UserRegistrationMain(@Autowired UserInformationService userInformationService,
      @Autowired RegistrationIdentity registrationIdentity,
      @Autowired AuthAttemptThrottle authAttemptThrottle) {
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "User Information service cannot be null");
    this.registrationSubmission = new RegistrationSubmission(
        Objects.requireNonNull(registrationIdentity, "registrationIdentity must not be null"),
        userInformationService,
        Objects.requireNonNull(authAttemptThrottle, "authAttemptThrottle must not be null"));

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

  private void onRegistrationClicked(UserRegistrationInformation information) {
    var failures = registrationSubmission.submitLocal(
        new RegistrationSubmission.RegistrationForm(information.fullName(), information.email(),
            information.userName(), information.password()),
        this::navigateToPendingConfirmation);
    if (!failures.isEmpty()) {
      handleRegistrationFailure(failures);
    }
  }

  private void navigateToPendingConfirmation(java.util.Optional<String> userId) {
    userId.ifPresentOrElse(
        id -> getUI().orElseThrow().navigate(EmailConfirmationMain.class, id),
        () -> getUI().orElseThrow().navigate(EmailConfirmationMain.class));
  }

  private void handleRegistrationFailure(List<RuntimeException> exceptionList) {
    /*These Cases should not happen anymore since we validate before we send the event,
    however they can still be used as a failsafe*/
    if (exceptionList.isEmpty()) {
      return;
    }
    for (RuntimeException e : exceptionList) {
      // UserExistsException is handled before this method is reached, by RegistrationOutcome, and
      // must never be rendered here: "email already in use" is the account-existence oracle
      // (AUTH-NFR-03). The branch was removed rather than left unreachable, so it cannot be restored
      // by accident while looking like a helpful default.
      if (e instanceof UserNameNotAvailableException) {
        userRegistrationComponent.showError("Username already in use",
            "Please try another username");
        break;
      } else if (e instanceof EmptyUserNameException) {
        userRegistrationComponent.showError("Username must not be empty",
            "Please try another username");
        break;
      } else if (e instanceof UserNameTooLongException) {
        userRegistrationComponent.showError("Username is too long",
            "User name must not exceed " + UserNames.MAX_LENGTH + " characters");
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
