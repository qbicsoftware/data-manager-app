package life.qbic.authentication.application.user.registration;

/**
 * <b>Confirm Email use case input</b>
 *
 * @since 1.0.0
 */
public interface ConfirmEmailInput {

  /**
   * Confirms the user's mail address
   *
   * @param userID the user whose mail address is to be confirmed
   * @since 1.0.0
   */
  void confirmEmailAddress(String userID);

}
