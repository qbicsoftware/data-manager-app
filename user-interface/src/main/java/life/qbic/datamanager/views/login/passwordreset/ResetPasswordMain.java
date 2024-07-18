package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.UserNotFoundException;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> Defines the look of the password reset layout. </b>
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
  private final ResetEmailSentComponent resetEmailSentComponent;
  private final ResetPasswordComponent resetPasswordComponent;

  public ResetPasswordMain(@Autowired IdentityService identityService,
      @Autowired ResetPasswordComponent resetPasswordComponent,
      @Autowired ResetEmailSentComponent resetEmailSentComponent) {
    this.identityService = Objects.requireNonNull(identityService,
        "Identity service cannot be null");
    this.resetPasswordComponent = Objects.requireNonNull(resetPasswordComponent,
        "ResetPasswordComponent cannot be null");
    this.resetEmailSentComponent = Objects.requireNonNull(resetEmailSentComponent,
        "ResetEmailSentComponent cannot be null");
    resetPasswordComponent.addResetPasswordListener(event -> resetPassword(event.getEmail()));
    resetEmailSentComponent.addLoginButtonListener(event -> {
      getUI().orElseThrow().navigate(LoginLayout.class);
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

  private void resetPassword(String email) {
    identityService.requestPasswordReset(email)
        .ifSuccessOrElse(response -> {
          resetPasswordComponent.clearNotifications();
          resetPasswordComponent.setVisible(false);
          resetEmailSentComponent.setVisible(true);
        }, response -> handleRegistrationFailure(response.failures()));
  }

  private void handleRegistrationFailure(List<RuntimeException> exceptionList) {
    if (exceptionList.isEmpty()) {
      return;
    }
    if (exceptionList.contains(EmailAddress.EmailValidationException.class)) {
      resetPasswordComponent.showError("Invalid mail address format",
          "Please provide a valid mail address.");
    } else if (exceptionList.contains(UserNotFoundException.class)) {
      resetPasswordComponent.showError(
          "User not found", "No user with the provided mail address is known.");
    } else if (exceptionList.contains(IdentityService.UserNotActivatedException.class)) {
      resetPasswordComponent.showError("User not active",
          "Please activate your account first to reset the password.");
    } else {
      resetPasswordComponent.showError(
          "An unexpected error occurred", "Please contact support@qbic.zendesk.com for help.");
    }
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
