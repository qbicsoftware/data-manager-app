package life.qbic.views.register;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import life.qbic.domain.usermanagement.registration.RegisterUserInput;
import life.qbic.domain.usermanagement.registration.RegisterUserOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles the {@link UserRegistrationLayout} components
 *
 * <p>This class is responsible for enabling buttons or triggering other view relevant changes on
 * the view class components
 */
@Component
public class UserRegistrationHandler
    implements UserRegistrationHandlerInterface, RegisterUserOutput {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger(UserRegistrationHandler.class);
  private UserRegistrationLayout userRegistrationLayout;

  private final RegisterUserInput registrationUseCase;

  @Autowired
  UserRegistrationHandler(RegisterUserInput registrationUseCase) {
    this.registrationUseCase = registrationUseCase;
    registrationUseCase.setOutput(this);
  }

  @Override
  public void handle(UserRegistrationLayout registrationLayout) {
    if (userRegistrationLayout != registrationLayout) {
      this.userRegistrationLayout = registrationLayout;
      initFields();
      addListener();
    }
  }

  private void initFields() {
    userRegistrationLayout.fullName.setPattern(".{1,}");
    userRegistrationLayout.fullName.setErrorMessage("A username must be at least 1 character");
    userRegistrationLayout.email.setErrorMessage("Please provide a valid email");
    userRegistrationLayout.password.setHelperText("A password must be at least 8 characters");
    userRegistrationLayout.password.setPattern(".{8,}");
    userRegistrationLayout.password.setErrorMessage("Password too short");
  }

  private void addListener() {
    userRegistrationLayout.registerButton.addClickShortcut(Key.ENTER);
    userRegistrationLayout.registerButton.addClickListener(
        event -> {
          resetComponentErrors();
          resetErrorMessages();
          registrationUseCase.register(
              userRegistrationLayout.fullName.getValue(),
              userRegistrationLayout.email.getValue(),
              userRegistrationLayout.password.getValue().toCharArray());
        });
  }

  private void resetErrorMessages() {
    userRegistrationLayout.alreadyUsedEmailMessage.setVisible(false);
    userRegistrationLayout.errorMessage.setVisible(false);
    userRegistrationLayout.passwordTooShortMessage.setVisible(false);
  }

  private void resetComponentErrors() {
    userRegistrationLayout.fullName.setErrorMessage("A username must be at least 1 character");
    userRegistrationLayout.email.setErrorMessage("Please provide a valid email");
    userRegistrationLayout.password.setErrorMessage("Password too short");
  }

  @Override
  public void onSuccess() {
    UI.getCurrent().navigate("/login");
  }

  @Override
  public void onFailure(String reason) {
    handleRegistrationFailure(reason);
  }

  private void handleRegistrationFailure(String reason) {
    if (reason.equals("Full Name shorter than 1 character.")) {
      userRegistrationLayout.fullName.setInvalid(true);
      userRegistrationLayout.fullName.setErrorMessage(reason);
    }
    if (reason.equals("Invalid email address format.")) {
      userRegistrationLayout.email.setInvalid(true);
      userRegistrationLayout.email.setErrorMessage(reason);
    }
    if (reason.equals("User with email address already exists.")) {
      userRegistrationLayout.alreadyUsedEmailMessage.setVisible(true);
      userRegistrationLayout.email.setInvalid(true);
      userRegistrationLayout.email.setErrorMessage(reason);
    }
    if (reason.equals("Unexpected error occurred.")) {
      userRegistrationLayout.errorMessage.setVisible(true);
    }
    if (reason.equals("Password shorter than 8 characters.")) {
      userRegistrationLayout.passwordTooShortMessage.setVisible(true);
      userRegistrationLayout.password.setInvalid(true);
      userRegistrationLayout.password.setErrorMessage(reason);
    }
  }
}
