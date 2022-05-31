package life.qbic.domain.usermanagement.registration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import life.qbic.apps.datamanager.services.UserRegistrationService;
import life.qbic.domain.usermanagement.User.UserException;

/**
 * <b>User Registration use case</b>
 *
 * <p>Tries to register a new user and create a user account.
 *
 * <p>In case a user with the provided email already exists, the registration will fail and calls
 * the failure output method.
 *
 * @since 1.0.0
 */
public class Registration implements RegisterUserInput {

  private RegisterUserOutput registerUserOutput;

  private final UserRegistrationService userRegistrationService;

  /**
   * Creates the registration use case.
   *
   * <p>Upon construction, a dummy output interface is created, that needs to be overridden by
   * explicitly setting it via {@link Registration#setRegisterUserOutput(RegisterUserOutput)}.
   *
   * <p>The default output implementation just prints to std out on success and std err on failure,
   * after the use case has been executed via
   * {@link Registration#register(String, String, char[])}.
   *
   * @param userRegistrationService the user registration service to save the new user to.
   * @since 1.0.0
   */
  public Registration(UserRegistrationService userRegistrationService) {
    this.userRegistrationService = userRegistrationService;
    // Init a dummy output, until one is set by the client.
    this.registerUserOutput =
        new RegisterUserOutput() {
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
  public void register(String fullName, String email, char[] rawPassword) {
    try {
      userRegistrationService.registerUser(fullName, email, rawPassword);
      registerUserOutput.onSuccess();
    } catch (UserException e) {
      if (containsSuppressedUserExceptions(e)) {
        listUserExceptionReasons(e).forEach(reason -> registerUserOutput.onFailure(reason));
      } else {
        registerUserOutput.onFailure(e.getReason());
      }
    } catch (Exception e) {
      registerUserOutput.onFailure("Unexpected error occurred.");
    }
  }

  private boolean containsSuppressedUserExceptions(UserException e) {
    return e.getSuppressed().length > 0;
  }

  private List<String> listUserExceptionReasons(UserException e) {
    List<String> userExceptionReasons = new ArrayList<>();
    Arrays.stream(e.getSuppressed()).toList()
        .forEach(userException -> userExceptionReasons.add(userException.getMessage()));
    return userExceptionReasons;
  }

  /**
   * @inheritDocs
   */
  @Override
  public void setOutput(RegisterUserOutput output) {
    registerUserOutput = output;
  }
}
