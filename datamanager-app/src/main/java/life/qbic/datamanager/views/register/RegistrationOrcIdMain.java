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
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.PermitAll;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.MainPage;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationOrcIdComponent.UserRegistrationOrcIdInformation;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserNames;
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException;
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException;
import life.qbic.identity.application.user.IdentityService.UserNameTooLongException;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route(value = AppRoutes.REGISTER_OIDC, layout = LandingPageLayout.class)
@PermitAll
public class RegistrationOrcIdMain extends Main implements BeforeEnterObserver,
    BeforeLeaveObserver {

  private static final Logger log = logger(RegistrationOrcIdMain.class);
  private final UserRegistrationOrcIdComponent userRegistrationOrcIdComponent;
  private final transient UserInformationService userInformationService;
  private final transient RegistrationSubmission registrationSubmission;

  public RegistrationOrcIdMain(
      @Autowired UserInformationService userInformationService,
      @Autowired RegistrationIdentity registrationIdentity,
      @Autowired AuthAttemptThrottle authAttemptThrottle) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.registrationSubmission = new RegistrationSubmission(
        requireNonNull(registrationIdentity, "registrationIdentity must not be null"),
        userInformationService,
        requireNonNull(authAttemptThrottle, "authAttemptThrottle must not be null"));
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
      return; // nothing to do
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      var failures = registrationSubmission.submitOidc(
          new RegistrationSubmission.RegistrationForm(information.fullName(), information.email(),
              information.userName(), ""),
          oidcUser.getIssuer().toString(),
          oidcUser.getName(),
          false,
          this::navigateToPendingConfirmation);
      if (!failures.isEmpty()) {
        handleRegistrationFailure(failures);
      }
      return;
    }
    UI.getCurrent().navigate(
        ProjectRoutes.PROJECTS); // no user id loaded as authentication principal is not overwritten.
  }

  /**
   * Sends the visitor to the pending-confirmation page, leaving the account id in the session.
   *
   * <p>The id must not travel in the URL: it is a persistent identifier that would land in browser
   * history, access logs and Referer headers, and anyone holding the link could press "Resend
   * verification link" to mail the account owner. The session scopes it to the visitor who just
   * registered, which is the only person the action is for.</p>
   *
   * <p>An absent id is not an error — it is the signature of the page opened without a preceding
   * registration, and the page handles it by offering no resend action.</p>
   */
  private void navigateToPendingConfirmation(Optional<String> userId) {
    VaadinSession session = VaadinSession.getCurrent();
    if (session != null) {
      session.setAttribute(EmailConfirmationMain.PENDING_REGISTRATION_USER_ID,
          userId.orElse(null));
    }
    getUI().orElseThrow().navigate(EmailConfirmationMain.class);
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
      // (AUTH-NFR-03). The branch was removed rather than left unreachable.
      if (e instanceof UserNameNotAvailableException) {
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
