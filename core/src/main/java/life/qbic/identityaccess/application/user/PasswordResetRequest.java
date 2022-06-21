package life.qbic.identityaccess.application.user;

import java.util.Objects;
import life.qbic.identityaccess.application.ApplicationResponse;

/**
 * <b>Password Reset Use Case</b>
 *
 * <p>Handles a user's password reset request.</p>
 *
 * @since 1.0.0
 */
class PasswordResetRequest implements PasswordResetInput {

  private PasswordResetOutput output;

  private UserRegistrationService registrationService;

  public PasswordResetRequest(UserRegistrationService registrationService) {
    super();
    this.registrationService = registrationService;
  }

  @Override
  public void resetPassword(String userId) {
    Objects.requireNonNull(output, "No use case output was set");
    // Todo trigger password reset
    ApplicationResponse response = registrationService.requestPasswordReset(userId);
    response.ifSuccessOrElse(success -> output.onPasswordResetSucceeded(),
        failure -> output.onPasswordResetFailed());
  }

  public void setUseCaseOutput(PasswordResetOutput output) {
    this.output = output;
  }


}
