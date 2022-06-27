package life.qbic.views.login.passwordreset;

import com.vaadin.flow.component.Key;
import life.qbic.identityaccess.application.user.PasswordResetInput;
import life.qbic.identityaccess.application.user.PasswordResetOutput;
import life.qbic.views.components.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>Handles the password reset</b>
 *
 * <p>When a password reset is triggered the handler starts the use case. On success the view is toggled
 * and the user can login again. On failure the user sees an error notification</p>
 *
 * @since 1.0.0
 */
@Component
public class PasswordResetHandler implements PasswordResetHandlerInterface, PasswordResetOutput {

    private ResetPasswordLayout registeredPasswordResetLayout;
    private final PasswordResetInput passwordReset;

    @Autowired
    PasswordResetHandler(PasswordResetInput passwordReset){
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
        registeredPasswordResetLayout.sendButton.addClickListener(buttonClickEvent ->
            passwordReset.resetPassword(registeredPasswordResetLayout.email.getValue()));
        registeredPasswordResetLayout.sendButton.addClickShortcut(Key.ENTER);

        registeredPasswordResetLayout.linkSentLayout.loginButton.addClickListener(buttonClickEvent ->
                registeredPasswordResetLayout.linkSentLayout.getUI().ifPresent(ui -> ui.navigate("login")));
    }
    public void clearNotifications() {
        registeredPasswordResetLayout.enterEmailLayout.removeNotifications();
    }

    public void showError(String title, String description) {
        clearNotifications();
        ErrorMessage errorMessage = new ErrorMessage(title, description);
        registeredPasswordResetLayout.enterEmailLayout.setNotification(errorMessage);
    }

    private void showPasswordResetFailedError() {
        showError("The Password Reset has failed", "Please try again.");
    }

    @Override
    public void onPasswordResetSucceeded() {
        registeredPasswordResetLayout.linkSentLayout.setVisible(true);
        registeredPasswordResetLayout.enterEmailLayout.setVisible(false);
    }

    @Override
    public void onPasswordResetFailed() {
        showPasswordResetFailedError();
    }
}
