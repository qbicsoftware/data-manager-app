package life.qbic.domain.usermanagement.registration;

import life.qbic.apps.datamanager.services.UserRegistrationException;
import life.qbic.apps.datamanager.services.UserRegistrationService;
import life.qbic.apps.datamanager.services.UserRegistrationService.RegistrationResponse;
import life.qbic.domain.user.Email.EmailValidationException;
import life.qbic.domain.user.EncryptedPassword.PasswordValidationException;
import life.qbic.domain.user.FullName.InvalidFullNameException;

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
   * after the use case has been executed via {@link Registration#register(String, String, char[])}.
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
          public void onFailure(UserRegistrationException e) {
            System.err.println("Called dummy register failure output.");
          }

          @Override
          public void onFailure(String reason) {

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
      RegistrationResponse registrationResponse = userRegistrationService.registerUser(fullName,
          email, rawPassword);
      if (registrationResponse.hasFailures()) {
        registerUserOutput.onFailure(build(registrationResponse));
      }
      registerUserOutput.onSuccess();
    } catch (Exception e) {
      e.printStackTrace();
      registerUserOutput.onFailure("Unexpected error occurred.");
    }
  }

  private UserRegistrationException build(RegistrationResponse registrationResponse) {
    var builder = UserRegistrationException.builder();

    for (Exception e : registrationResponse.failures()) {
      if (e.getClass().isInstance(EmailValidationException.class))
        builder.withEmailFormatException(e);
      if (e.getClass().isInstance(PasswordValidationException.class))
        builder.withInvalidPasswordException(e);
      if (e.getClass().isInstance(InvalidFullNameException.class))
        builder.withFullNameException(e);
      else
        builder.withUnexpectedException(e);
    }
    return builder.build();
  }

  /**
   * @inheritDocs
   */
  @Override
  public void setOutput(RegisterUserOutput output) {
    registerUserOutput = output;
  }
}
