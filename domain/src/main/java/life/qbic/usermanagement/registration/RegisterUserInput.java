package life.qbic.usermanagement.registration;

import life.qbic.usermanagement.User;

/**
 * Input interface to register a new user in the application.
 *
 * @since 1.0.0
 */
public interface RegisterUserInput {

  void register(User user);

  void setOutput(RegisterUserOutput output);

}
