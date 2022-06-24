package life.qbic.views.login.passwordreset;

import life.qbic.identityaccess.application.user.PasswordResetInput;
import life.qbic.identityaccess.application.user.PasswordResetOutput;
import org.apache.commons.lang3.NotImplementedException;
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

        registeredPasswordResetLayout.linkSent.loginButton.addClickListener(buttonClickEvent ->
                registeredPasswordResetLayout.linkSent.getUI().ifPresent(ui -> ui.navigate("login")));
    }

    @Override
    public void onPasswordResetSucceeded() {
        registeredPasswordResetLayout.linkSent.setVisible(true);
        registeredPasswordResetLayout.enterEmailLayout.setVisible(false);
    }

    @Override
    public void onPasswordResetFailed() {
        //todo
        throw new NotImplementedException();
    }
}
