package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.Objects;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> Defines the look of the password reset layout.
 * It hosts the {@link ResetPasswordComponent} to enable the user to input the email account which
 * should receive a password reset email and a {@link ResetEmailSentComponent} component informing
 * the user that an email has been sent to the provided email account /b>
 *
 * @since 1.0.0
 */
@PageTitle("Reset Password")
@Route(value = AppRoutes.RESET_PASSWORD, layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
public class ResetPasswordMain extends Main implements BeforeEnterObserver {

  private static final Logger log =
      LoggerFactory.logger(ResetPasswordMain.class.getName());
  private final transient IdentityService identityService;
  private final transient AuthAttemptThrottle authAttemptThrottle;
  private final ResetEmailSentComponent resetEmailSentComponent;
  private final ResetPasswordComponent resetPasswordComponent;

  public ResetPasswordMain(@Autowired IdentityService identityService,
      @Autowired ResetPasswordComponent resetPasswordComponent,
      @Autowired ResetEmailSentComponent resetEmailSentComponent,
      @Autowired AuthAttemptThrottle authAttemptThrottle) {
    this.identityService = Objects.requireNonNull(identityService,
        "Identity service cannot be null");
    this.authAttemptThrottle = Objects.requireNonNull(authAttemptThrottle,
        "authAttemptThrottle cannot be null");
    this.resetPasswordComponent = Objects.requireNonNull(resetPasswordComponent,
        "ResetPasswordComponent cannot be null");
    this.resetEmailSentComponent = Objects.requireNonNull(resetEmailSentComponent,
        "ResetEmailSentComponent cannot be null");
    resetPasswordComponent.addResetPasswordListener(event -> resetPassword(event.getEmail()));
    resetEmailSentComponent.addLoginButtonListener(event -> {
      UI.getCurrent().navigate(AppRoutes.LOGIN);
      resetEmailSentComponent.setVisible(false);
      resetPasswordComponent.setVisible(true);
    });
    addClassName("reset-password");
    add(resetEmailSentComponent, resetPasswordComponent);
    log.debug(String.format(
        "New instance for %s(#%s) created with %s(#%s) and %s(#%s)",
        this.getClass().getSimpleName(), System.identityHashCode(this),
        resetPasswordComponent.getClass().getSimpleName(),
        System.identityHashCode(resetPasswordComponent),
        resetEmailSentComponent.getClass().getSimpleName(),
        System.identityHashCode(resetEmailSentComponent)));
  }

  /**
   * Requests a password reset.
   *
   * <p>The response is <b>always</b> "an email has been sent, if an account exists". AUTH-NFR-03
   * forbids disclosing whether an account exists, whether it is confirmed, or whether it is
   * otherwise eligible — so a registered address, an unknown address and an unconfirmed account all
   * produce the same screen. This replaced an earlier version that surfaced "User not found" and
   * "User not active" verbatim, which turned the form into an account-enumeration oracle.</p>
   *
   * <p>Only a malformed address is reported, because that is a property of the input the visitor
   * typed, not of any account.</p>
   */
  void resetPassword(String email) {
    boolean syntacticallyValid = isPlausibleEmailAddress(email);
    if (!syntacticallyValid) {
      resetPasswordComponent.showError("Invalid mail address format",
          "Please provide a valid mail address.");
      return;
    }

    // Server-side throttle (AUTH-NFR-03, #1682). Note what is deliberately NOT here: no branch on
    // the outcome. A "too many requests" screen would confirm that this address is real and that the
    // throttle is keyed on it, which is exactly the oracle the uniform response exists to remove
    // (decision B4). The visitor sees the same "check your inbox" panel either way, and the only
    // difference is whether an email is actually sent.
    //
    // ⚠️ EmailConfirmationMain makes the OPPOSITE choice — it tells the visitor to wait. That is not
    // an inconsistency: this page is reachable by anyone with no prior proof of anything, so a
    // distinguishing message is an oracle. That page is only reachable after the visitor proved they
    // know the user id, so stating a cooldown there discloses nothing and silence would just look
    // like a broken mail server. Do not "unify" the two without checking that reasoning first.
    //
    // record(), not isThrottled()+record(): the attempt that crosses the threshold is itself over
    // the limit, so only record()'s return value correctly refuses it.
    var decision = authAttemptThrottle.record(AuthAttemptThrottle.Scope.PASSWORD_RESET_RESEND, email,
        java.time.Instant.now());
    if (decision == AuthAttemptThrottle.Decision.ALLOWED) {
      try {
        identityService.requestPasswordReset(email);
      } catch (RuntimeException e) {
        // Swallowed on purpose: an unexpected failure must not become an oracle either. It is logged
        // for operators, and the visitor sees the same generic outcome as everyone else.
        log.error("Password reset request failed unexpectedly: " + e, e);
      }
    }

    resetPasswordComponent.clearNotifications();
    resetPasswordComponent.setVisible(false);
    resetEmailSentComponent.setVisible(true);
  }

  /**
   * Minimal syntactic check. Deliberately not a validity check against a real mailbox: the point is
   * only to reject obviously malformed input, which is not an account-state question.
   */
  private static boolean isPlausibleEmailAddress(String email) {
    if (email == null) {
      return false;
    }
    String trimmed = email.strip();
    int at = trimmed.indexOf('@');
    return at > 0 && at == trimmed.lastIndexOf('@') && at < trimmed.length() - 1
        && trimmed.indexOf(' ') < 0;
  }

  /**
   * Callback executed before navigation to attaching Component chain is made.
   *
   * @param event before navigation event with event details
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    resetPasswordComponent.setVisible(true);
    resetEmailSentComponent.setVisible(false);
  }
}
