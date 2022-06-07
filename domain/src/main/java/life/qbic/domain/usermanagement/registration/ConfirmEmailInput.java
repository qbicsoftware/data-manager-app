package life.qbic.domain.usermanagement.registration;

/**
 * <b>Confirm EmailAddress use case input</b>
 *
 * @since 1.0.0
 */
public interface ConfirmEmailInput {

  /**
   * Confirms the user's email address
   *
   * @param userID the user whose email address is to be confirmed
   * @since 1.0.0
   */
  void confirmEmailAddress(String userID);

}
