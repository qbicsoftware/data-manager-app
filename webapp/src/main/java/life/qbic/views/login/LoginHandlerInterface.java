package life.qbic.views.login;

/**
 * <b> Interface to register the {@link LoginLayout} to the {@link LoginHandler}. </b>
 *
 * @since 1.0.0
 */
public interface LoginHandlerInterface {

  /**
   * Register the {@link LoginLayout} to the implementing class
   * @param loginView The view that is being registered
   * @return true, if registration was successful
   */
  boolean register(LoginLayout loginView);

}
