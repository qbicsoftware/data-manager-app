package life.qbic.authentication.application.user.registration;

import life.qbic.application.commons.ApplicationResponse;
import life.qbic.authentication.application.user.registration.UserRegistrationService.UserExistsException;
import life.qbic.authentication.domain.user.concept.EmailAddress.EmailValidationException;
import life.qbic.authentication.domain.user.concept.EncryptedPassword.PasswordValidationException;
import life.qbic.authentication.domain.user.concept.FullName.FullNameValidationException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

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

  private static final Logger log = LoggerFactory.logger(Registration.class.getName());

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
    if (registerUserOutput == null) {
      log.error("No use case output set.");
      return;
    }
    try {
      userRegistrationService.registerUser(fullName, email, rawPassword)
          .ifSuccessOrElse(this::reportSuccess,
              response -> registerUserOutput.onUnexpectedFailure(build(response)));
    } catch (Exception e) {
      log.error("User registration failed", e);
      registerUserOutput.onUnexpectedFailure("Unexpected error occurred.");
    }
  }

  private void reportSuccess(ApplicationResponse applicationResponse) {
    registerUserOutput.onUserRegistrationSucceeded();
  }

  private UserRegistrationException build(ApplicationResponse applicationResponse) {
    var builder = UserRegistrationException.builder();

    for (RuntimeException e : applicationResponse.failures()) {
      if (e instanceof EmailValidationException) {
        builder.withEmailFormatException((EmailValidationException) e);
      } else if (e instanceof PasswordValidationException) {
        builder.withInvalidPasswordException((PasswordValidationException) e);
      } else if (e instanceof FullNameValidationException) {
        builder.withFullNameException((FullNameValidationException) e);
      } else if (e instanceof UserExistsException) {
        builder.withUserExistsException((UserExistsException) e);
      } else {
        builder.withUnexpectedException(e);
      }
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
