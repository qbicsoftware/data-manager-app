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
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserNames;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserExistsException;
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
  private final transient IdentityService identityService;
  private final transient UserInformationService userInformationService;
  private final transient AuthAttemptThrottle authAttemptThrottle;
  private final UserRegistrationComponent userRegistrationComponent;

  public UserRegistrationMain(@Autowired IdentityService identityService, @Autowired
  UserInformationService userInformationService,
      @Autowired AuthAttemptThrottle authAttemptThrottle) {
    this.identityService = Objects.requireNonNull(identityService,
        "Identity service cannot be null");
    this.userInformationService = Objects.requireNonNull(userInformationService,
        "User Information service cannot be null");
    this.authAttemptThrottle = Objects.requireNonNull(authAttemptThrottle,
        "authAttemptThrottle must not be null");

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
    // Throttle before doing any work (AUTH-NFR-03, FEAT-AUTH-FLOWS-03).
    //
    // Registration is an unauthenticated request surface, and since the duplicate-address response was
    // made uniform it also sends mail. Two abuses it would otherwise allow: mass-submitting addresses to
    // make the application send notification email, and repeatedly invalidating a victim's outstanding
    // reset link, because each newly issued token supersedes the previous one.
    //
    // Throttled submissions are reported exactly like a successful one — see the note on the uniform
    // outcome below. Stating the throttle here would restore the account-existence oracle through the
    // back door, since the bucket is keyed on the submitted address and consulted before the lookup.
    var throttleOutcome = authAttemptThrottle.recordAndReport(
        AuthAttemptThrottle.Scope.REGISTRATION, userRegistrationInformation.email(),
        Instant.now());
    if (!throttleOutcome.isAllowed()) {
      log.debug("Registration submission throttled");
      redirectToPendingConfirmation(userRegistrationInformation.email());
      return;
    }

    identityService.registerUser(
            userRegistrationInformation.fullName(),
            userRegistrationInformation.userName(),
            userRegistrationInformation.email(),
            userRegistrationInformation.password().toCharArray())
        .ifSuccessOrElse(applicationResponse -> redirectToPendingConfirmation(
                userRegistrationInformation.email()),
            applicationResponse -> {
              if (isDuplicateAccount(applicationResponse.failures())) {
                // Uniform outcome (AUTH-NFR-03, FEAT-AUTH-FLOWS-03).
                //
                // A second registration for an address that already has an account must be
                // indistinguishable from a successful one: same navigation, same page, same wording.
                // Saying "Email address already in use" here would be the same enumeration oracle the
                // field validator used to be, one step later.
                //
                // The owner learns about it by email instead, through the ordinary password-reset
                // notification the domain already dispatches. Two facts make that safe and honest:
                //   * the notice states that it can be ignored if the recipient did not trigger it,
                //     so a person who never registered is not alarmed or sent hunting for a password
                //     they never had;
                //   * the registration scope is throttled, so an attacker cannot use this to spam an
                //     inbox or to keep invalidating the owner's outstanding reset link.
                notifyAccountOwnerOfRepeatedRegistration(userRegistrationInformation.email());
                redirectToPendingConfirmation(userRegistrationInformation.email());
                return;
              }
              handleRegistrationFailure(applicationResponse.failures());
            });
  }

  /**
   * Whether the failures are "an account already exists for this address".
   *
   * <p>Matched on the exception type rather than on rendered text, so the uniform response cannot be
   * defeated by a message change elsewhere.</p>
   */
  private static boolean isDuplicateAccount(List<RuntimeException> failures) {
    return failures.stream().anyMatch(UserExistsException.class::isInstance);
  }

  /**
   * Sends the owner of an existing account the standard reset notice, telling them it can be ignored.
   *
   * <p>Reuses the password-reset notification deliberately: it is the one existing message that gives
   * the owner a way forward (set a new password) without disclosing anything to the requester, and it
   * is what the previous error text already pointed at.</p>
   */
  private void notifyAccountOwnerOfRepeatedRegistration(String email) {
    var response = identityService.requestPasswordReset(email);
    if (response.hasFailures()) {
      log.debug("Repeated registration for an existing account; notification not dispatched");
    }
  }

  /**
   * Navigates to the "check your inbox" page for the given address, regardless of whether the
   * registration created an account.
   *
   * <p>The page is reached with a real user id when an account exists so that "resend" works. For a
   * duplicate registration the id is that of the existing account — the visitor already knows the
   * address, and the page discloses nothing about it beyond what they typed. If the address somehow
   * has no account (registration failed for an unrelated reason that we nevertheless report
   * uniformly) the page is still shown, without an id, which the view already handles.</p>
   */
  private void redirectToPendingConfirmation(String email) {
    var userId = userInformationService.findByEmail(email).map(UserInfo::id);
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
