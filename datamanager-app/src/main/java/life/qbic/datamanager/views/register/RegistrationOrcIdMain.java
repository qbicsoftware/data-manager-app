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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.MainPage;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationComponent.Mode;
import life.qbic.datamanager.views.register.UserRegistrationComponent.UserRegistrationInformation;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserNames;
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
  private final UserRegistrationComponent userRegistrationComponent;
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
    // The SAME component as the local form, in the ORCID mode. It used to be a parallel class with a
    // copy of the validator chain; see UserRegistrationComponent.
    userRegistrationComponent = new UserRegistrationComponent(userInformationService, Mode.ORCID);
    userRegistrationComponent.addRegistrationListener(
        event -> registerOidcUser(event.information()));
    add(userRegistrationComponent);
    addClassName("user-registration");
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        userRegistrationComponent.getClass().getSimpleName(),
        System.identityHashCode(userRegistrationComponent)));
  }

  private static String buildFullName(String givenName, String middleName, String familyName) {
    return "%s%s%s".formatted(
        isNull(givenName) ? "" : givenName,
        isNull(middleName) ? "" : " " + middleName,
        isNull(familyName) ? "" : " " + familyName
    );
  }

  private void registerOidcUser(UserRegistrationInformation information) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicOidcUser) {
      return; // nothing to do
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      var failures = registrationSubmission.submitOidc(
          // No password: this mode does not collect one. The form reports an empty value, and
          // RegistrationSubmission documents that the ORCID flow registers without one.
          RegistrationSubmission.RegistrationForm.of(information),
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

  /**
   * Renders the failures the submission returned, and logs them.
   *
   * <p>The mapping to text lives in {@link RegistrationFailureMessage}, which both registration routes
   * share; this method only renders and logs.</p>
   */
  private void handleRegistrationFailure(List<RuntimeException> exceptionList) {
    if (exceptionList.isEmpty()) {
      return;
    }
    RegistrationFailureMessage.render(userRegistrationComponent, exceptionList.getFirst());
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
      userRegistrationComponent.setFullName(
          buildFullName(oidcUser.getGivenName(), oidcUser.getMiddleName(),
              oidcUser.getFamilyName()));
      Optional.ofNullable(oidcUser.getEmail()).ifPresent(userRegistrationComponent::setEmail);
      Optional.ofNullable(oidcUser.getPreferredUsername())
          .map(RegistrationOrcIdMain::truncateToUserNameLimit)
          .ifPresent(
              userRegistrationComponent::setUsername);
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
