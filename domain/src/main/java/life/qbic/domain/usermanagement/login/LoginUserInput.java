package life.qbic.domain.usermanagement.login;

/**
 * Input interface to login an existing user in the application.
 *
 * @since 1.0.0
 */
public interface LoginUserInput {

  /**
   * Login using the email and password provided to this method.
   *
   * @param email    the email of the user trying to log in.
   * @param password the password of the user trying to log in.
   * @since 1.0.0
   */
  void login(String email, String password);

  /**
   * Set the output the use case shall call, when finished.
   *
   * @param output the output to call when the user logged in
   * @since 1.0.0
   */
  void setOutput(LoginUserOutput output);

}
