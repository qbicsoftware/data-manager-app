package life.qbic.authentication.application.user.password;

/**
 * New Password use case input
 *
 * @since 1.0.0
 */
public interface NewPasswordInput {

  /**
   * Sets a new password for a given user.
   *
   * @param userId         the user's id
   * @param newRawPassword the new raw password
   * @since 1.0.0
   */
  void setNewUserPassword(String userId, char[] newRawPassword);

}
