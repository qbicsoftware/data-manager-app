package life.qbic.domain.usermanagement.registration;

import java.util.Objects;
import life.qbic.apps.datamanager.services.UserRegistrationService;
import life.qbic.domain.usermanagement.repository.UserRepository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EmailAddressConfirmation implements ConfirmEmailInput {

  private ConfirmEmailOutput confirmEmailOutput;

  private final UserRegistrationService userRegistrationService;

  public EmailAddressConfirmation(UserRegistrationService userRegistrationService) {
    this.userRegistrationService = Objects.requireNonNull(userRegistrationService);
  }

  public void setConfirmEmailOutput(ConfirmEmailOutput confirmEmailOutput){
    this.confirmEmailOutput = confirmEmailOutput;
  }

  @Override
  public void confirmEmailAddress(String userID) {
    Objects.requireNonNull(confirmEmailOutput, "No use case output was set yet");
    try  {
      userRegistrationService.activateUser(userID);
      confirmEmailOutput.onSuccess();
    } catch (Exception ignored) {
      confirmEmailOutput.onFailure("Email address could not be confirmed");
    }
  }
}
