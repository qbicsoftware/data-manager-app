package life.qbic.domain.usermanagement.login;


/**
 * Output interface for the user login use case
 *
 * @since 1.0.0
 */
public interface LoginUserOutput {

  /**
   * Actions performed when the user login succeeded.
   *
   * @since 1.0.0
   */
  void onLoginSucceeded();

  /**
   * Actions performed when the user login failed.
   *
   * @since 1.0.0
   */
  void onLoginFailed();


}
