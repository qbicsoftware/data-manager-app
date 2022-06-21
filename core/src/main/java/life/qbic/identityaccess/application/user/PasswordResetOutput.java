package life.qbic.identityaccess.application.user;

/**
 * <b>Password Reset Use Case Output</b>
 *
 * <p>Defines call back methods for different use case outcomes.</p>
 *
 * @since 1.0.0
 */
public interface PasswordResetOutput {

  /**
   * Called by the use case, when the password reset was successful.
   *
   * @since 1.0.0
   */
  void onPasswordResetSucceeded();

  /**
   * Called by the use case, when the password reset failed.
   *
   * @since 1.0.0
   */
  void onPasswordResetFailed();
}
