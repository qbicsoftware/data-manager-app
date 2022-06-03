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
    userRegistrationLayout.fullName.setPattern("\\S.*");
    userRegistrationLayout.fullName.setErrorMessage("Please provide your full name here");
    userRegistrationLayout.email.setErrorMessage("Please provide a valid email address");
    userRegistrationLayout.password.setHelperText("A password must be at least 8 characters");
    userRegistrationLayout.password.setPattern(".{8,}");
    userRegistrationLayout.password.setErrorMessage("Password too short");
  }

  private void addListener() {
    userRegistrationLayout.registerButton.addClickShortcut(Key.ENTER);
    userRegistrationLayout.registerButton.addClickListener(
        event -> {
          resetErrorMessages();
          setEmptyInputInvalid();
          if (isUserInputValid()) {
            registrationUseCase.register(
                userRegistrationLayout.fullName.getValue(),
                userRegistrationLayout.email.getValue(),
                userRegistrationLayout.password.getValue().toCharArray());
          } else {
            userRegistrationLayout.invalidCredentialsMessage.setVisible(true);
          }
        });
  }

  private boolean isUserInputValid() {
    return !(userRegistrationLayout.fullName.isInvalid() || userRegistrationLayout.email.isInvalid()
        || userRegistrationLayout.password.isInvalid());
  }

  private void setEmptyInputInvalid() {
    if (userRegistrationLayout.fullName.isEmpty()) {
      userRegistrationLayout.fullName.setInvalid(true);
    }
    if (userRegistrationLayout.email.isEmpty()) {
      userRegistrationLayout.email.setInvalid(true);
    }
    if (userRegistrationLayout.password.isEmpty()) {
      userRegistrationLayout.password.setInvalid(true);
    }
  }

  private void resetErrorMessages() {
    userRegistrationLayout.alreadyUsedEmailMessage.setVisible(false);
    userRegistrationLayout.errorMessage.setVisible(false);
    userRegistrationLayout.passwordTooShortMessage.setVisible(false);
    userRegistrationLayout.invalidCredentialsMessage.setVisible(false);
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
    switch (reason) {
      case "Full Name shorter than 1 character." ->
          userRegistrationLayout.fullName.setInvalid(true);
      case "Invalid email address format." -> userRegistrationLayout.email.setInvalid(true);
      case "User with email address already exists." -> {
        userRegistrationLayout.alreadyUsedEmailMessage.setVisible(true);
        userRegistrationLayout.email.setInvalid(true);
      }
      case "Password shorter than 8 characters." -> {
        userRegistrationLayout.passwordTooShortMessage.setVisible(true);
        userRegistrationLayout.password.setInvalid(true);
      }
      default -> userRegistrationLayout.errorMessage.setVisible(true);
    }
  }
}
