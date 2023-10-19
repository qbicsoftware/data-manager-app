package life.qbic.identity.application.user.password;

import java.util.function.Predicate;
import life.qbic.application.commons.Result;
import life.qbic.identity.application.user.registration.UserRegistrationService;
import life.qbic.identity.domain.model.EncryptedPassword;
import life.qbic.identity.domain.model.EncryptedPassword.PasswordValidationException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;

/**
 * <b>New password use case</b>
 * <p>
 * Set's a new password for a user.
 *
 * @since 1.0.0
 */
public class NewPassword implements NewPasswordInput {

  private static final Logger log = LoggerFactory.logger(NewPassword.class.getName());
  private final UserRegistrationService userRegistrationService;
  private NewPasswordOutput useCaseOutput;

  public NewPassword(UserRegistrationService userRegistrationService) {
    super();
    this.userRegistrationService = userRegistrationService;
  }

  public void setUseCaseOutput(NewPasswordOutput useCaseOutput) {
    this.useCaseOutput = useCaseOutput;
  }

  @Override
  public void setNewUserPassword(String userId, char[] newRawPassword) {
    if (useCaseOutput == null) {
      log.error("No use case output was set");
      return;
    }

    Result<EncryptedPassword, RuntimeException> response = userRegistrationService.newUserPassword(
        userId, newRawPassword);
    Predicate<RuntimeException> isPasswordValidationException = e -> e instanceof PasswordValidationException;
    response
        .onValue(ignored -> {
          log.info(String.format("Successful password reset for user %s", userId));
          useCaseOutput.onSuccessfulNewPassword();
        })
        .onErrorMatching(isPasswordValidationException, ignored -> {
          log.error(String.format("Could not set new password for user: %s", userId));
          useCaseOutput.onPasswordValidationFailure();
        })
        .onErrorMatching(isPasswordValidationException.negate(), ignored -> {
          log.error(String.format("Unexpected failure on password reset for user: %s", userId));
          useCaseOutput.onUnexpectedFailure();
        });
  }
}
