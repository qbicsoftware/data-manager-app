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

  private final RegisterUserOutput registerUserOutput;

  private final UserDataStorage userRepository;

  public Registration(RegisterUserOutput registerUserOutput, UserDataStorage userRepository) {
    this.registerUserOutput = registerUserOutput;
    this.userRepository = userRepository;
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

  private boolean userExists(User user) {
    return !userRepository.findUsersByEmail(user.getEmail()).isEmpty();
  }
}
