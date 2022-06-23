package life.qbic.views.login.resetPassword;

import life.qbic.views.register.UserRegistrationLayout;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface PasswordResetHandlerInterface {

    /**
     * Registers a {@link UserRegistrationLayout} to an implementing class
     *
     * @param registerLayout The view that is being handled
     * @since 1.0.0
     */
    void handle(EnterEmailLayout registerLayout);
}
