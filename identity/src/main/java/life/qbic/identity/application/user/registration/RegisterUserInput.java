package life.qbic.identity.application.user.registration;

/**
 * Input interface to register a new user in the application.
 *
 * @since 1.0.0
 */
public interface RegisterUserInput {

  /**
   * Registers a new user in the application.
   *
   * <p>The raw password passed needs to be cleared, after it has been successfully processed.
   *
   * @param fullName    the full name of the user
   * @param email       the user's mail address
   * @param rawPassword the user selected raw password for authentication
   * @param userName    the unique user name specified by the user
   * @since 1.0.0
   */
  void register(String fullName, String email, char[] rawPassword, String userName);

  /**
   * Set the output the use case shall call, when finished.
   *
   * @param output the output to call when the registration has been performed
   * @since 1.0.0
   */
  void setOutput(RegisterUserOutput output);
}
