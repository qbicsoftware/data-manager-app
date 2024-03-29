package life.qbic.identity.application.user.registration;

import java.util.Objects;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.UserNotFoundException;

/**
 * <b>Email Address Confirmation use case</b>
 * <p>
 * Activates a user with a given user id.
 *
 * @since 1.0.0
 */
public class EmailAddressConfirmation implements ConfirmEmailInput {

  private ConfirmEmailOutput confirmEmailOutput;

  private final IdentityService identityService;

  public EmailAddressConfirmation(IdentityService identityService) {
    this.identityService = Objects.requireNonNull(identityService);
  }

  public void setConfirmEmailOutput(ConfirmEmailOutput confirmEmailOutput) {
    this.confirmEmailOutput = confirmEmailOutput;
  }

  @Override
  public void confirmEmailAddress(String userID) {
    Objects.requireNonNull(confirmEmailOutput, "No use case output was set yet");
    try {
      identityService.confirmUserEmail(userID);
      confirmEmailOutput.onEmailConfirmationSuccess();
    } catch (UserNotFoundException e) {
      confirmEmailOutput.onEmailConfirmationFailure("Unknown user");
    }
  }
}
