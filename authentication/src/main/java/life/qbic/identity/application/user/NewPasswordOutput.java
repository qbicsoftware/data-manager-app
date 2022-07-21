package life.qbic.identity.application.user;

/**
 * New Password use case output
 *
 * @since 1.0.0
 */
public interface NewPasswordOutput {

  /**
   * Called when the new password has been saved successfully
   *
   * @since 1.0.0
   */
  void onSuccessfulNewPassword();

  /**
   * Called, when the password did not meet the requirements
   *
   * @since 1.0.0
   */
  void onPasswordValidationFailure();

  /**
   * Called, when an unexpected failure occurred.
   *
   * @since 1.0.0
   */
  void onUnexpectedFailure();

}
