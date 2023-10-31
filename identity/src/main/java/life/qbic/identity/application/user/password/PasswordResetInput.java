package life.qbic.identity.application.user.password;

/**
 * <b>Password Reset Use Case Input</b>
 *
 * <p>Requests a user password reset.</p>
 *
 * @since 1.0.0
 */
public interface PasswordResetInput {

  /**
   * Requests a password reset for a user identified by the user id.
   *
   * @param emailAddress the user's mail address
   * @since 1.0.0
   */
  void resetPassword(String emailAddress);

}
