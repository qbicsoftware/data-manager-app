package life.qbic.views.login;

/**
 * <b> Interface to handle the {@link LoginLayout} to the {@link LoginHandler}. </b>
 *
 * @since 1.0.0
 */
public interface LoginHandlerInterface {

    /**
     * Register the {@link LoginLayout} to the implementing class
     *
     * @param loginView The view that is being registered
     * @since 1.0.0
     */
    void handle(LoginLayout loginView);

}
