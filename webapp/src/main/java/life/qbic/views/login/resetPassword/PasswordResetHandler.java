package life.qbic.views.login.resetPassword;

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

    @Override
    public void handle(EnterEmailLayout layout) {
        if (registeredPasswordResetLayout != layout) {
            this.registeredPasswordResetLayout = layout;
            //addClickListeners();
        }
    }
}
