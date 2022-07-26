package life.qbic.datamanager.views.login.passwordreset;

/**
 * <b>Handles the {@link ResetPasswordLayout} components</b>
 *
 * <p>This class is responsible for enabling buttons or triggering other view relevant changes on
 * the view class components
 *
 * @since 1.0.0
 */
public interface PasswordResetHandlerInterface {

    /**
     * Registers a {@link ResetPasswordLayout} to an implementing class
     *
     * @param registerLayout The view that is being handled
     * @since 1.0.0
     */
    void handle(ResetPasswordLayout registerLayout);
}
