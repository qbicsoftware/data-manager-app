package life.qbic.views.register;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import life.qbic.usermanagement.User;
import life.qbic.usermanagement.User.UserException;
import life.qbic.usermanagement.registration.RegisterUserInput;
import life.qbic.usermanagement.registration.RegisterUserOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles the {@link UserRegistrationLayout} components
 *
 * <p>This class is responsible for enabling buttons or triggering other view relevant changes on
 * the view class components
 */
@Component
public class UserRegistrationHandler implements UserRegistrationHandlerInterface, RegisterUserOutput {

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
  public boolean handle(UserRegistrationLayout registrationLayout) {
    if (userRegistrationLayout != registrationLayout) {
      this.userRegistrationLayout = registrationLayout;
      // orchestrate view
      initFields();
      addListener();
      // then return
      return true;
    }
    return false;
  }

  private void initFields () {
    userRegistrationLayout.password.setHelperText("A password must be at least 8 characters");
    userRegistrationLayout.password.setPattern(".{8,}");
    userRegistrationLayout.password.setErrorMessage("Password too short");
  }

  private void addListener() {
    userRegistrationLayout.registerButton.addClickShortcut(Key.ENTER);

    userRegistrationLayout.registerButton.addClickListener(event -> {
      resetErrorMessages();
      try {
        var user = User.create(
            userRegistrationLayout.password.getValue(),
            userRegistrationLayout.fullName.getValue(),
            userRegistrationLayout.email.getValue());
        registrationUseCase.register(user);
      } catch (UserException e) {
        handleUserException(e.getMessage());
      }
    });
  }

  private void handleUserException(String reason) {
    if (reason.equalsIgnoreCase("Password shorter than 8 characters.")) {
      userRegistrationLayout.passwordTooShortMessage.setVisible(true);
    }
  }

  private void resetErrorMessages() {
    userRegistrationLayout.passwordTooShortMessage.setVisible(false);
    userRegistrationLayout.alreadyUsedEmailMessage.setVisible(false);
  }

  @Override
  public void onSuccess() {
    UI.getCurrent().navigate("/login");
  }

  @Override
  public void onFailure(String reason) {
    // Display error to the user
    // Stub output:
    System.out.println(reason);
    userRegistrationLayout.alreadyUsedEmailMessage.setVisible(true);
  }
}
