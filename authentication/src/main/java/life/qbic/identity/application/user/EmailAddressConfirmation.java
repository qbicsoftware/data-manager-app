package life.qbic.identity.application.user;

import java.util.Objects;
import life.qbic.identity.domain.user.UserNotFoundException;

/**
 * <b>Email Address Confirmation use case</b>
 * <p>
 * Activates a user with a given user id.
 *
 * @since 1.0.0
 */
public class EmailAddressConfirmation implements ConfirmEmailInput {

  private ConfirmEmailOutput confirmEmailOutput;

  private final UserRegistrationService userRegistrationService;

  public EmailAddressConfirmation(UserRegistrationService userRegistrationService) {
    this.userRegistrationService = Objects.requireNonNull(userRegistrationService);
  }

  public void setConfirmEmailOutput(ConfirmEmailOutput confirmEmailOutput) {
    this.confirmEmailOutput = confirmEmailOutput;
  }

  @Override
  public void confirmEmailAddress(String userID) {
    Objects.requireNonNull(confirmEmailOutput, "No use case output was set yet");
    try {
      userRegistrationService.confirmUserEmail(userID);
      confirmEmailOutput.onEmailConfirmationSuccess();
    } catch (UserNotFoundException e) {
      confirmEmailOutput.onEmailConfirmationFailure("Unknown user");
    }
  }
}
