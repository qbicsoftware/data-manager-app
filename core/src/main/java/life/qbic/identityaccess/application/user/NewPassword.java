package life.qbic.identityaccess.application.user;

import life.qbic.identityaccess.domain.user.EncryptedPassword.PasswordValidationException;
import life.qbic.shared.application.ApplicationResponse;

/**
 * <b>New password use case</b>
 * <p>
 * Set's a new password for a user.
 *
 * @since 1.0.0
 */
public class NewPassword implements NewPasswordInput {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewPassword.class);
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

    ApplicationResponse response = userRegistrationService.newUserPassword(userId, newRawPassword);

    response.ifSuccessOrElse(ignored -> {
          log.info("Successful password reset for user " + userId);
          useCaseOutput.onSuccessfulNewPassword();
        },
        it -> it.failures().stream().filter(e -> e instanceof PasswordValidationException).findAny()
            .ifPresentOrElse(ignored -> {
                  log.error("Could not set new password for user: " + userId);
                  useCaseOutput.onPasswordValidationFailure();
                },
                () -> {
                  log.error("Unexpected failure on password reset for user: " + userId);
                  useCaseOutput.onUnexpectedFailure();
                }));

  }
}
