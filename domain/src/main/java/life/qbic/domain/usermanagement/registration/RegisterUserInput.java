package life.qbic.domain.usermanagement.registration;

import life.qbic.domain.usermanagement.User;

/**
 * Input interface to register a new user in the application.
 *
 * @since 1.0.0
 */
public interface RegisterUserInput {

  /**
   * Register a new user.
   *
   * @param user the new user to register
   * @since 1.0.0
   */
  void register(User user);

  /**
   * Set the output the use case shall call, when finished.
   * @param output the output to call when the registration has been performed
   * @since 1.0.0
   */
  void setOutput(RegisterUserOutput output);

}
