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
import java.util.Timer;
import java.util.TimerTask;
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

  private int lockoutTimer = 0;

  private UserInfo userInfo;

  public EmailConfirmationMain(UserInformationService userInformationService,
      EmailService emailService,
      EmailConfirmationLinkSupplier emailConfirmationLinkSupplier) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.emailService = requireNonNull(emailService, "emailService must not be null");
    this.emailConfirmationLinkSupplier = requireNonNull(emailConfirmationLinkSupplier,
        "emailConfirmationLinkSupplier must not be null");
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
      if (lockoutTimer > 0) {
        showLockoutNotification(lockoutTimer);
        return;
      }
      sendConfirmationEmail(userInfo.id(), userInfo.emailAddress(), userInfo.fullName());
      scheduleLockoutTimer();
      showLockoutNotification(lockoutTimer);
    });
    resendVerification.addClassName("resend-verification");
    emailConfirmationComponent.add(resendVerification);

    RouterLink backToLoginLink = new RouterLink("Go back to login", LoginLayout.class);
    emailConfirmationComponent.add(backToLoginLink);
  }

  private void scheduleLockoutTimer() {
    final int VERIFICATION_EMAIL_LOCKOUT_SECONDS_TIMER = 30;
    lockoutTimer = VERIFICATION_EMAIL_LOCKOUT_SECONDS_TIMER;
    Timer timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        lockoutTimer--;
        if (lockoutTimer <= 0) {
          timer.cancel();
          clearNotifications();
        }
      }
    }, 1, 1000);
  }

  private void showLockoutNotification(int timeLeft) {
    showInfo("Verification email has been sent",
        "Please wait " + timeLeft + " seconds until another email can be sent");
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
    var content = new Content(Messages.formatRegistrationEmailContent(fullName,
        emailConfirmationLinkSupplier.emailConfirmationUrl(userId)));
    try {
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
