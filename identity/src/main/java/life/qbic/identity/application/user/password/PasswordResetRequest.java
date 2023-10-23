package life.qbic.identity.application.user.password;

import java.util.Objects;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.identity.application.user.IdentityService;

/**
 * <b>Password Reset Use Case</b>
 *
 * <p>Handles a user's password reset request.</p>
 *
 * @since 1.0.0
 */
public class PasswordResetRequest implements PasswordResetInput {

  private PasswordResetOutput output;

  private final IdentityService registrationService;

  public PasswordResetRequest(IdentityService registrationService) {
    super();
    this.registrationService = registrationService;
  }

  @Override
  public void resetPassword(String emailAddress) {
    Objects.requireNonNull(output, "No use case output was set");
    ApplicationResponse response = registrationService.requestPasswordReset(emailAddress);
    response.ifSuccessOrElse(success -> output.onPasswordResetSucceeded(),
        failure -> output.onPasswordResetFailed(failure));
  }

  public void setUseCaseOutput(PasswordResetOutput output) {
    this.output = output;
  }
}
