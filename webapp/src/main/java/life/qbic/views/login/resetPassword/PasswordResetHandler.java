package life.qbic.views.login.resetPassword;

import life.qbic.identityaccess.application.user.PasswordResetInput;
import life.qbic.identityaccess.application.user.PasswordResetOutput;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */ //todo
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
            //addClickListeners();
            registeredPasswordResetLayout.sendButton.addClickListener(buttonClickEvent -> {
                passwordReset.resetPassword(registeredPasswordResetLayout.email.getValue());
            });
            registeredPasswordResetLayout.linkSent.loginButton.addClickListener(buttonClickEvent ->
                    registeredPasswordResetLayout.linkSent.getUI().ifPresent(ui -> ui.navigate("login")));
        }
    }

    @Override
    public void onPasswordResetSucceeded() {
        registeredPasswordResetLayout.linkSent.setVisible(true);
        registeredPasswordResetLayout.enterEmailLayout.setVisible(false);
    }

    @Override
    public void onPasswordResetFailed() {
        //todo
        //throw new NotImplementedException();
    }
}
