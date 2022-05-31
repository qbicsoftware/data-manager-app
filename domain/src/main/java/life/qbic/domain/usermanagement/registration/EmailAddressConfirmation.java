package life.qbic.domain.usermanagement.registration;

import java.util.Objects;
import life.qbic.apps.datamanager.services.UserRegistrationService;
import life.qbic.domain.usermanagement.repository.UserRepository;

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
      confirmEmailOutput.onSuccess();
    } catch (UserNotFoundException e) {
      confirmEmailOutput.onFailure("Unknown user");
    }
  }
}
