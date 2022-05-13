package life.qbic.domain.usermanagement.registration;

import life.qbic.domain.usermanagement.User;
import life.qbic.domain.usermanagement.repository.UserRepository;

/**
 * <b>User Registration use case</b>
 * <p>
 * Tries to register a new user and create a user account.
 * <p>
 * In case a user with the provided email already exists, the registration will fail and calls the
 * failure output method.
 *
 * @since 1.0.0
 */
public class Registration implements RegisterUserInput {

  private RegisterUserOutput registerUserOutput;

  private final UserRepository userRepository;

  /**
   * Creates the registration use case.
   * <p>
   * Upon construction, a dummy output interface is created, that needs to be overridden by
   * explicitly setting it via {@link Registration#setRegisterUserOutput(RegisterUserOutput)}.
   * <p>
   * The default output implementation just prints to std out on success and std err on failure,
   * after the use case has been exectuted via {@link Registration#register(User)}.
   *
   * @param userRepository the user repository to save the new user to.
   * @since 1.0.0
   */
  public Registration(UserRepository userRepository) {
    this.userRepository = userRepository;
    // Init a dummy output, until one is set by the client.
    this.registerUserOutput = new RegisterUserOutput() {
      @Override
      public void onSuccess() {
        System.out.println("Called dummy register success output.");
      }

      @Override
      public void onFailure(String reason) {
        System.err.println("Called dummy register failure output.");
      }
    };
  }

  /**
   * Sets and overrides the use case output.
   *
   * @param registerUserOutput an output interface implementation, so the use case can trigger the
   *                           callback methods after its execution
   * @since 1.0.0
   */
  public void setRegisterUserOutput(RegisterUserOutput registerUserOutput) {
    this.registerUserOutput = registerUserOutput;
  }

  /**
   * @inheritDocs
   */
  @Override
  public void register(User user) {
    userRepository.findByEmail(user.getEmail())
        .ifPresentOrElse(
            u -> registerUserOutput.onFailure("User with email address already exists."),
            () -> {
              user.setEmailConfirmed(false);
              userRepository.addUser(user);
              registerUserOutput.onSuccess();
            });
  }

  /**
   * @inheritDocs
   */
  @Override
  public void setOutput(RegisterUserOutput output) {
    registerUserOutput = output;
  }

  private boolean userExists(User user) {
    return userRepository.findByEmail(user.getEmail()).isPresent();
  }
}
