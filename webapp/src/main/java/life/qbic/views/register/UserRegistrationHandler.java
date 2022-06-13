package life.qbic.views.register;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import life.qbic.apps.datamanager.services.UserRegistrationException;
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

  @Override
  public void onUserRegistrationSucceeded() {
    UI.getCurrent().navigate("/login");
  }

  @Override
  public void onUnexpectedFailure(UserRegistrationException userRegistrationException) {
    handleRegistrationFailure(userRegistrationException);
  }

  @Override
  public void onUnexpectedFailure(String reason) {
    userRegistrationLayout.errorMessage.setVisible(true);
  }

  private void handleRegistrationFailure(UserRegistrationException userRegistrationException) {
    if (userRegistrationException.fullNameException().isPresent()) {
      userRegistrationLayout.fullName.setInvalid(true);
    }
    if (userRegistrationException.passwordException().isPresent()) {
      userRegistrationLayout.password.setInvalid(true);
      userRegistrationLayout.passwordTooShortMessage.setVisible(true);
    }
    if (userRegistrationException.emailFormatException().isPresent()) {
      userRegistrationLayout.email.setInvalid(true);
    }
    if (userRegistrationException.userExistsException().isPresent()) {
      userRegistrationLayout.alreadyUsedEmailMessage.setVisible(true);
    }
    if (userRegistrationException.unexpectedException().isPresent()) {
      userRegistrationLayout.errorMessage.setVisible(true);
    }
  }
}
