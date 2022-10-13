package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.Key;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.authentication.application.user.password.PasswordResetInput;
import life.qbic.authentication.application.user.password.PasswordResetOutput;
import life.qbic.authentication.application.user.registration.UserRegistrationService;
import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.repository.UserNotFoundException;
import life.qbic.datamanager.views.components.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Handles the password reset</b>
 *
 * <p>When a password reset is triggered the handler starts the use case. On success the view is
 * toggled and the user can login again. On failure the user sees an error notification
 *
 * @since 1.0.0
 */
@Component
public class PasswordResetHandler implements PasswordResetHandlerInterface, PasswordResetOutput {

  private ResetPasswordLayout registeredPasswordResetLayout;
  private final PasswordResetInput passwordReset;

  @Autowired
  PasswordResetHandler(PasswordResetInput passwordReset) {
    this.passwordReset = passwordReset;
  }

  @Override
  public void handle(ResetPasswordLayout layout) {
    if (registeredPasswordResetLayout != layout) {
      this.registeredPasswordResetLayout = layout;
      addClickListeners();
    }
  }

  private void addClickListeners() {
    registeredPasswordResetLayout.sendButton.addClickListener(
        buttonClickEvent -> {
          clearNotifications();
          passwordReset.resetPassword(registeredPasswordResetLayout.email.getValue());
        });
    registeredPasswordResetLayout.sendButton.addClickShortcut(Key.ENTER);

    registeredPasswordResetLayout.linkSentLayout.loginButton.addClickListener(
        buttonClickEvent ->
            registeredPasswordResetLayout
                .linkSentLayout
                .getUI()
                .ifPresent(ui -> ui.navigate("login")));
  }

  public void clearNotifications() {
    registeredPasswordResetLayout.enterEmailLayout.removeNotifications();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    registeredPasswordResetLayout.enterEmailLayout.setNotification(errorMessage);
  }

  private void showPasswordResetFailedError(String error, String description) {
    showError(error, description);
  }

  @Override
  public void onPasswordResetSucceeded() {
    registeredPasswordResetLayout.linkSentLayout.setVisible(true);
    registeredPasswordResetLayout.enterEmailLayout.setVisible(false);
  }

  @Override
  public void onPasswordResetFailed(ApplicationResponse response) {
    for (RuntimeException failure : response.failures()) {
      if (failure instanceof EmailAddress.EmailValidationException) {
        showPasswordResetFailedError("Invalid mail address format", "Please provide a valid mail address.");
      }
      else if (failure instanceof UserNotFoundException) {
        showPasswordResetFailedError(
            "User not found", "No user with the provided mail address is known.");
      }
      else if (failure instanceof UserRegistrationService.UserNotActivatedException) {
        showPasswordResetFailedError("User not active", "Please activate your account first to reset the password.");
      } else {
        showPasswordResetFailedError(
            "An unexpected error occurred", "Please contact support@qbic.zendesk.com for help.");
      }
    }
  }
}
