package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.Duration;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.MainPage;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.communication.Content;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.Messages;
import life.qbic.identity.application.communication.Recipient;
import life.qbic.identity.application.communication.Subject;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import life.qbic.projectmanagement.application.communication.CommunicationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The page to be shown after registration when the user needs to confirm the email.
 *
 * @since 1.1.0
 */
@Route(value = AppRoutes.EMAIL_CONFIRMATION, layout = LandingPageLayout.class)
@PageTitle("Waiting for email confirmation")
@AnonymousAllowed
public class EmailConfirmationMain extends Main implements BeforeEnterObserver {

  /**
   * Session key under which the registration flow leaves the id of the account it just created.
   *
   * <p><b>Why not in the URL.</b> The page used to take the user id as a route parameter, giving
   * {@code /register/pending-email-confirmation/<uuid>}. That exposed a persistent identifier in browser
   * history, server access logs and {@code Referer} headers — the same identifier the old confirmation
   * links used as the credential itself — and it meant anyone holding the URL could press "Resend
   * verification link" and mail the account owner at will.</p>
   *
   * <p>The id is only needed to enable that one action, for the visitor who just registered in this very
   * session. Session state is the right scope: it is not persisted, not bookmarked, not logged, and it
   * disappears with the session.</p>
   */
  public static final String PENDING_REGISTRATION_USER_ID =
      EmailConfirmationMain.class.getName() + ".pendingUserId";

  private static final Logger log = logger(EmailConfirmationMain.class);

  private final transient UserInformationService userInformationService;

  private final Div emailConfirmationComponent = new Div();

  private final Div notificationLayout = new Div();

  private final transient EmailService emailService;

  private final transient EmailConfirmationLinkSupplier emailConfirmationLinkSupplier;

  private final transient life.qbic.identity.api.VerificationLinkService verificationLinkService;

  private final transient AuthAttemptThrottle authAttemptThrottle;

  private UserInfo userInfo;

  public EmailConfirmationMain(UserInformationService userInformationService,
      EmailService emailService,
      EmailConfirmationLinkSupplier emailConfirmationLinkSupplier,
      life.qbic.identity.api.VerificationLinkService verificationLinkService,
      AuthAttemptThrottle authAttemptThrottle) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.emailService = requireNonNull(emailService, "emailService must not be null");
    this.emailConfirmationLinkSupplier = requireNonNull(emailConfirmationLinkSupplier,
        "emailConfirmationLinkSupplier must not be null");
    this.verificationLinkService = requireNonNull(verificationLinkService,
        "verificationLinkService must not be null");
    this.authAttemptThrottle = requireNonNull(authAttemptThrottle,
        "authAttemptThrottle must not be null");
    initConfirmEmailComponent();
    addClassName("email-confirmation");
    add(emailConfirmationComponent);
  }

  private void initConfirmEmailComponent() {
    emailConfirmationComponent.addClassName("email-confirmation-component");
    H2 title = new H2("Verify your email");
    emailConfirmationComponent.add(title);
    emailConfirmationComponent.add(notificationLayout);
    Text description = new Text(
        "We've sent a verification link to your email. Please check your inbox and click on the link to complete the registration");
    emailConfirmationComponent.add(description);
    // The one sentence that makes this page honest for BOTH outcomes of a registration submission.
    //
    // A registration against an address that already has an account is deliberately indistinguishable
    // from a new one (AUTH-NFR-03), so this page is reached in two situations and must state something
    // true in both. The first sentence is conditional for that reason — on the duplicate path no
    // verification email is sent at all, because no account was created.
    //
    // The second sentence is what stops the duplicate path from being a dead end. Without it, a person
    // who mistyped their own address, or forgot they had an account, was told to check an inbox that
    // will never receive anything and given no way forward. It is shown to everyone, so it discloses
    // nothing, and it deliberately points at the reset flow rather than sending mail from here: a
    // notification triggered by somebody else typing an address would describe an action the owner
    // never took and would invalidate any reset link they were genuinely waiting for.
    // Wrapped in a Span, not added as a bare Text: a Text is a client-side text node and cannot carry a
    // CSS class, so styling it would silently do nothing (or throw, as it did under test).
    Span alreadyRegistered = new Span(
        "If that address was already registered, no new email will arrive. If you already have an "
            + "account, use \"Forgot your password\" on the login page to regain access.");
    alreadyRegistered.addClassName("already-registered-hint");
    emailConfirmationComponent.add(alreadyRegistered);
    Text resendVerificationText = new Text("Did not receive an email?");
    Span resendVerificationLink = new Span("Resend verification link");
    resendVerificationLink.addClassName("link");
    Span resendVerification = new Span(resendVerificationText, resendVerificationLink);
    resendVerificationLink.addClickListener(spanClickEvent -> {
      // Resend with no user id must not throw.
      //
      // The pending user id comes from the session, not the URL, so opening this page directly (a
      // bookmark, a pasted link, a reloaded tab after the session expired) leaves it null. Clicking
      // "Resend" used to dereference it and produce a NullPointerException -> HTTP 500. There is no
      // account context to resend for, so say so plainly instead of failing.
      if (userInfo == null) {
        showError("Cannot resend the link",
            "Please open the link from your registration email, or register again.");
        return;
      }

      // Server-side throttle (AUTH-NFR-03, #1682). This replaces a 30-second client-side
      // java.util.Timer whose counter lived in a UIScope bean: reloading the page reset it to zero,
      // so it was not a control at all.
      //
      // recordAndReport(), not record(): the view needs the attempt counts to tell the visitor how
      // many attempts remain, so they do not discover the limit by hitting it.
      var outcome = authAttemptThrottle.recordAndReport(
          AuthAttemptThrottle.Scope.EMAIL_CONFIRMATION_RESEND, userInfo.emailAddress(),
          java.time.Instant.now());

      if (outcome.isAllowed()) {
        sendConfirmationEmail(userInfo.id(), userInfo.emailAddress(), userInfo.fullName());
        showInfo("Verification email sent", successCopy(outcome));
        return;
      }

      // Throttled: say so, and say when to come back.
      //
      // This is deliberately NOT hidden, and the reason is specific to this page: it is only
      // reachable after the visitor proved they know the user id (UserRegistrationMain navigates
      // here with a real id straight after registering). Everyone here has already demonstrated the
      // account exists, so stating "wait N minutes" discloses nothing that the URL did not.
      //
      // Staying silent would be worse than useless: the visitor would be told an email was sent, no
      // email would arrive, and they would report a broken mail server. See ResetPasswordMain for the
      // opposite decision, where the audience is anonymous and silence IS the requirement.
      Duration cooldown = authAttemptThrottle.blockDuration(
          AuthAttemptThrottle.Scope.EMAIL_CONFIRMATION_RESEND);
      showInfo("Verification email already sent",
          "To prevent duplicate emails, another link can be requested in %s. Check your inbox and "
              .formatted(cooldownText(cooldown))
              + "spam folder first — the link from the previous email is still valid.");
    });
    resendVerification.addClassName("resend-verification");
    emailConfirmationComponent.add(resendVerification);

    RouterLink backToLoginLink = new RouterLink("Go back to login", LoginLayout.class);
    emailConfirmationComponent.add(backToLoginLink);
  }

  /**
   * Copy for a successful resend, including how many attempts remain.
   *
   * <p>The remaining count matters because without it there is no signal that a limit exists: a
   * visitor who clicks three times sees "sent" three times and only meets the limit on the fourth
   * click, which reads as a malfunction. Naming the allowance makes the behaviour predictable.</p>
   *
   * <p>On the last permitted attempt the copy changes, because "0 more" reads as an error.</p>
   */
  static String successCopy(AuthAttemptThrottle.Outcome outcome) {
    int remaining = outcome.attemptsRemainingInWindow();
    if (remaining <= 0) {
      return "Please check your inbox and click the link to complete your registration. "
          + "This was the last link that can be requested for now.";
    }
    return ("Please check your inbox and click the link to complete your registration. "
        + "You can request %s more %s.")
        .formatted(remaining, remaining == 1 ? "link" : "links");
  }

  /**
   * Renders a duration for a human, without seconds-level precision nobody needs.
   *
   * <p>Deliberately coarse: the throttle window is deployment configuration and can change, so the
   * copy must not hard-code "15 minutes" and then drift.</p>
   */
  static String cooldownText(Duration cooldown) {
    long minutes = cooldown.toMinutes();
    if (minutes < 1) {
      return "a moment";
    }
    if (minutes == 1) {
      return "1 minute";
    }
    if (minutes < 60) {
      return minutes + " minutes";
    }
    long hours = cooldown.toHours();
    return hours == 1 ? "1 hour" : hours + " hours";
  }

  private boolean isAlreadyActiveUser(QbicOidcUser oidcUser) {
    return isAlreadyActiveUser(oidcUser.getQbicUserId());
  }

  private boolean isAlreadyActiveUser(QbicUserDetails qbicUserDetails) {
    return isAlreadyActiveUser(qbicUserDetails.getUserId());
  }

  private boolean isAlreadyActiveUser(String userId) {
    Optional<UserInfo> request = userInformationService.findById(userId);
    return request.map(UserInfo::isActive).orElse(false);
  }

  private void sendConfirmationEmail(String userId, String email, String fullName) {
    var subject = new Subject("Please confirm your email address");
    var recipient = new Recipient(email, fullName);
    try {
      // Issue a fresh single-use token. The user id must never be used as the token: it is not a
      // credential, it is derivable, and it is visible in ACL records. Issuing here also supersedes
      // any earlier confirmation token for this account, so an older link cannot stay alive
      // alongside the new one (AUTH-R-05).
      var issued = verificationLinkService.issue(userId,
          life.qbic.identity.api.VerificationLinkService.Purpose.EMAIL_CONFIRMATION,
          java.time.Instant.now());
      var content = new Content(Messages.formatRegistrationEmailContent(fullName,
          emailConfirmationLinkSupplier.emailConfirmationUrl(issued.rawToken())));
      emailService.send(subject, recipient, content);
    } catch (CommunicationException communicationException) {
      showError("Couldn't send email verification", "Please try again");
      log.error(communicationException.getMessage());
    }
  }

  private void clearNotifications() {
    notificationLayout.removeAll();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    notificationLayout.add(errorMessage);
  }

  public void showInfo(String title, String description) {
    clearNotifications();
    InformationMessage informationMessage = new InformationMessage(title, description);
    notificationLayout.add(informationMessage);
  }

  /**
   * Notifies about navigating to the target that implements this interface.
   *
   * @param event     the navigation event that caused the call to this method
   * @param parameter the resolved url parameter
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    forwardToMainIfAlreadyConfirmed(event, null);
    // The id lives in the session, never in the URL (see PENDING_REGISTRATION_USER_ID). It is absent on a
    // fresh session, a bookmark, or a URL someone pasted elsewhere — in which case the page simply shows no
    // resend action, which is the safe default.
    Object pending = VaadinSession.getCurrent() == null
        ? null
        : VaadinSession.getCurrent().getAttribute(PENDING_REGISTRATION_USER_ID);
    if (pending instanceof String userId && !userId.isBlank()) {
      var optUserInfo = userInformationService.findById(userId);
      if (optUserInfo.isPresent()) {
        userInfo = optUserInfo.get();
      } else {
        log.debug("Pending registration id no longer resolves to an account; resend disabled");
        VaadinSession.getCurrent().setAttribute(PENDING_REGISTRATION_USER_ID, null);
      }
    }
  }

  private void forwardToMainIfAlreadyConfirmed(BeforeEvent event, String parameter) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (isNull(authentication)) {
      log.debug(
          "user without authentication and parameter [%s] reached the confirm email page".formatted(
              parameter));
      return;
    }
    var principal = authentication.getPrincipal();
    // no idea how they ended up here but the account is already active, so they can go to the main page directly
    if (principal instanceof QbicOidcUser qbicOidcUser && isAlreadyActiveUser(qbicOidcUser)) {
      event.forwardTo(MainPage.class);
    }
    if (principal instanceof QbicUserDetails qbicUserDetails && isAlreadyActiveUser(
        qbicUserDetails)) {
      event.forwardTo(MainPage.class);
    }
  }
}
