package life.qbic.usermanagement.registration;

import life.qbic.usermanagement.User;
import life.qbic.usermanagement.repository.UserDataStorage;

/**
 * <b>User Registration use case</b>
 * <p>
 * Tries to register a new user and create a user account.
 * <p>
 * In case a user with the provided email already exists, the registration will fail and calls the
 * failure output method.
 *
 * @since 1.5.0
 */
public class Registration implements RegisterUserInput {

  private RegisterUserOutput registerUserOutput;

  private final UserDataStorage userRepository;

  public Registration(UserDataStorage userRepository) {
    this.userRepository = userRepository;
  }

  public void setRegisterUserOutput(RegisterUserOutput registerUserOutput) {
    this.registerUserOutput = registerUserOutput;
  }

  @Override
  public void register(User user) {
    if (userExists(user)) {
      registerUserOutput.onFailure("User with email address already exists.");
    } else {
      // Register the user
      user.setEmailConfirmed(false);
      userRepository.save(user);
      // Then execute success callback
      registerUserOutput.onSuccess();
    }
  }

  @Override
  public void setOutput(RegisterUserOutput output) {
    registerUserOutput = output;
  }

  private boolean userExists(User user) {
    return !userRepository.findUsersByEmail(user.getEmail()).isEmpty();
  }
}
