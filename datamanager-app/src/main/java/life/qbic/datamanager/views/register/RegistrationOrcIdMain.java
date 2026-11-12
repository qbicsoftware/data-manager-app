package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.MainPage;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationOrcIdComponent.UserRegistrationOrcIdInformation;
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
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route(value = AppRoutes.REGISTER_OIDC, layout = LandingPageLayout.class)
@PermitAll
public class RegistrationOrcIdMain extends Main implements BeforeEnterObserver,
    BeforeLeaveObserver {

  private static final Logger log = logger(RegistrationOrcIdMain.class);
  private final transient IdentityService identityService;
  private final UserRegistrationOrcIdComponent userRegistrationOrcIdComponent;
  private final transient UserInformationService userInformationService;
  private final transient AuthAttemptThrottle authAttemptThrottle;

  public RegistrationOrcIdMain(
      @Qualifier("userRegistrationService") IdentityService identityService,
      @Autowired UserInformationService userInformationService,
      @Autowired AuthAttemptThrottle authAttemptThrottle) {
    this.identityService = requireNonNull(identityService, "identityService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.authAttemptThrottle = requireNonNull(authAttemptThrottle,
        "authAttemptThrottle must not be null");
    userRegistrationOrcIdComponent = new UserRegistrationOrcIdComponent(userInformationService);
    userRegistrationOrcIdComponent.addRegistrationListener(
        event -> registerOidcUser(event.userRegistrationOrcIdInformation()));
    add(userRegistrationOrcIdComponent);
    addClassName("user-registration");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        userRegistrationOrcIdComponent.getClass().getSimpleName(),
        System.identityHashCode(userRegistrationOrcIdComponent)));
  }

  private static String buildFullName(String givenName, String middleName, String familyName) {
    return "%s%s%s".formatted(
        isNull(givenName) ? "" : givenName,
        isNull(middleName) ? "" : " " + middleName,
        isNull(familyName) ? "" : " " + familyName
    );
  }

  private void registerOidcUser(UserRegistrationOrcIdInformation information) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicOidcUser) {
      return; //nothing to do
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      // Throttle before any work — same reasoning as UserRegistrationMain. Reported identically.
      var throttleOutcome = authAttemptThrottle.recordAndReport(
          AuthAttemptThrottle.Scope.REGISTRATION, information.email(), Instant.now());
      if (!throttleOutcome.isAllowed()) {
        log.debug("ORCID registration submission throttled");
        redirectToPendingConfirmation(information.email());
        return;
      }

      ApplicationResponse registrationResponse = identityService.registerOpenIdUser(
          information.fullName(),
          information.userName(), information.email(), oidcUser.getIssuer().toString(),
          oidcUser.getName());
      registrationResponse.ifSuccessOrElse(
          response -> redirectToPendingConfirmation(information.email()),
          response -> {
            if (isDuplicateAccount(response.failures())) {
              // Uniform outcome, same reasoning as UserRegistrationMain: a duplicate registration must
              // not be distinguishable from a successful one. The owner is notified by email instead.
              notifyAccountOwnerOfRepeatedRegistration(information.email());
              redirectToPendingConfirmation(information.email());
              return;
            }
            handleRegistrationFailure(response.failures());
          }
      );
      return;
    }
    UI.getCurrent().navigate(
        ProjectRoutes.PROJECTS); // no user id loaded as authentication principal is not overwritten.
  }

  private void handleRegistrationFailure(List<RuntimeException> exceptionList) {
    /*These Cases should not happen anymore since we validate before we send the event,
    however they can still be used as a failsafe*/
    if (exceptionList.isEmpty()) {
      return;
    }
    for (RuntimeException e : exceptionList) {
      if (e instanceof UserExistsException) {
        userRegistrationOrcIdComponent.showError("Email address already in use",
            "If you have difficulties with your password you can reset it.");
        break;
      } else if (e instanceof UserNameNotAvailableException) {
        userRegistrationOrcIdComponent.showError("Username already in use",
            "Please try another username");
        break;
      } else if (e instanceof EmptyUserNameException) {
        userRegistrationOrcIdComponent.showError("Username must not be empty",
            "Please try another username");
        break;
      } else if (e instanceof UserNameTooLongException) {
        userRegistrationOrcIdComponent.showError("Username is too long",
            "User name must not exceed " + UserNames.MAX_LENGTH + " characters");
        break;
      } else {
        userRegistrationOrcIdComponent.showError("Registration failed", "Please try again.");
        break;
      }
    }
    String allErrorMessages = exceptionList.stream().map(Throwable::getMessage)
        .collect(Collectors.joining("\n"));
    log.error(allErrorMessages);
    exceptionList.forEach(e -> log.debug(e.getMessage(), e));
  }

  /**
   * Whether the failures are "an account already exists for this address".
   *
   * <p>Matched on the exception type so the uniform response does not depend on rendered text.</p>
   */
  private static boolean isDuplicateAccount(List<RuntimeException> failures) {
    return failures.stream().anyMatch(UserExistsException.class::isInstance);
  }

  /**
   * Sends the owner of an existing account the standard reset notice, telling them it can be ignored.
   *
   * <p>Reuses the password-reset notification deliberately — see {@code UserRegistrationMain}.</p>
   */
  private void notifyAccountOwnerOfRepeatedRegistration(String email) {
    var response = identityService.requestPasswordReset(email);
    if (response.hasFailures()) {
      log.debug("Repeated registration for an existing account; notification not dispatched");
    }
  }

  /**
   * Navigates to the "check your inbox" page for the given address, whether or not it has an account.
   *
   * @see UserRegistrationMain#redirectToPendingConfirmation(String)
   */
  private void redirectToPendingConfirmation(String email) {
    var userId = userInformationService.findByEmail(email).map(UserInfo::id);
    userId.ifPresentOrElse(
        id -> getUI().orElseThrow().navigate(EmailConfirmationMain.class, id),
        () -> getUI().orElseThrow().navigate(EmailConfirmationMain.class));
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicOidcUser qbicOidcUser) {
      log.warn("User %s tried to enter registration although exists.".formatted(
          qbicOidcUser.getQbicUserId()));
      event.rerouteTo(MainPage.class);
      return;
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      //note: for ORCID, only given name and family name are supported https://orcid.org/.well-known/openid-configuration.
      userRegistrationOrcIdComponent.setFullName(
          buildFullName(oidcUser.getGivenName(), oidcUser.getMiddleName(),
              oidcUser.getFamilyName()));
      Optional.ofNullable(oidcUser.getEmail()).ifPresent(userRegistrationOrcIdComponent::setEmail);
      Optional.ofNullable(oidcUser.getPreferredUsername())
          .map(RegistrationOrcIdMain::truncateToUserNameLimit)
          .ifPresent(
              userRegistrationOrcIdComponent::setUsername);
    }
  }

  /**
   * The ORCID {@code preferred_username} claim may exceed the platform's username length limit.
   * Truncating it keeps the prefill a valid starting point instead of pre-filling the form with a
   * value that cannot be submitted. The user remains free to edit it before registering.
   */
  private static String truncateToUserNameLimit(String preferredUsername) {
    String stripped = preferredUsername.strip();
    return stripped.length() <= UserNames.MAX_LENGTH
        ? stripped
        : stripped.substring(0, UserNames.MAX_LENGTH);
  }

  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    SecurityContextHolder.getContext().setAuthentication(null); // remove authentication
  }
}
