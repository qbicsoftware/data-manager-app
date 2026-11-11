package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
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
public class EmailConfirmationMain extends Main implements HasUrlParameter<String> {

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
    Text resendVerificationText = new Text("Did not receive an email?");
    Span resendVerificationLink = new Span("Resend verification link");
    resendVerificationLink.addClassName("link");
    Span resendVerification = new Span(resendVerificationText, resendVerificationLink);
    resendVerificationLink.addClickListener(spanClickEvent -> {
      // Server-side throttle (AUTH-NFR-03, #1682).
      //
      // This replaces a 30-second client-side java.util.Timer whose counter lived in a UIScope
      // bean: reloading the page reset it to zero, so it was not a control at all. The timer is
      // gone rather than kept alongside, because two mechanisms disagreeing about whether a resend
      // is allowed is worse than one.
      //
      // The DECISION decides whether the work happens. It must not decide what is rendered — see
      // below. Note it is record() and not isThrottled()+record(): the attempt that crosses the
      // threshold is itself over the limit, so only record()'s return value correctly refuses it.
      var decision = authAttemptThrottle.record(
          AuthAttemptThrottle.Scope.EMAIL_CONFIRMATION_RESEND, userInfo.emailAddress(),
          java.time.Instant.now());
      if (decision == AuthAttemptThrottle.Decision.ALLOWED) {
        sendConfirmationEmail(userInfo.id(), userInfo.emailAddress(), userInfo.fullName());
      }
      // One message for both outcomes. "Verification email has been sent" is true enough for the
      // throttled case too — an email was sent recently — and discloses nothing. A distinct "too
      // many requests" panel would confirm to an attacker that the address is real and that the
      // throttle is keyed on it (decision B4).
      showInfo("Verification email has been sent",
          "Please check your inbox. If the email does not arrive, wait a few minutes before "
              + "requesting another one.");
    });
    resendVerification.addClassName("resend-verification");
    emailConfirmationComponent.add(resendVerification);

    RouterLink backToLoginLink = new RouterLink("Go back to login", LoginLayout.class);
    emailConfirmationComponent.add(backToLoginLink);
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
  public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
    forwardToMainIfAlreadyConfirmed(event, parameter);
    if (parameter != null && !parameter.isEmpty()) {
      var optUserInfo = userInformationService.findById(parameter);
      if (optUserInfo.isEmpty()) {
        log.error("Invalid UserId provided during pending email confirmation "
            + optUserInfo);
        throw new NotFoundException("Could not find user");
      } else {
        userInfo = optUserInfo.get();
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
