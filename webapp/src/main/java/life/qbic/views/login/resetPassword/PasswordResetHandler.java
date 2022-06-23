package life.qbic.views.login.resetPassword;

import life.qbic.identityaccess.application.user.PasswordResetInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Component
public class PasswordResetHandler implements PasswordResetHandlerInterface{

    private EnterEmailLayout registeredPasswordResetLayout;
    private final PasswordResetInput passwordReset;

    @Autowired
    PasswordResetHandler(PasswordResetInput passwordReset){
        this.passwordReset = passwordReset;
    }

    @Override
    public void handle(EnterEmailLayout layout) {
        if (registeredPasswordResetLayout != layout) {
            this.registeredPasswordResetLayout = layout;
            //addClickListeners();
            registeredPasswordResetLayout.sendButton.addClickListener(buttonClickEvent -> {
                passwordReset.resetPassword();
                registeredPasswordResetLayout.getUI().ifPresent(ui -> ui.navigate("account-recovery/sent"));
            });
        }
    }
}
