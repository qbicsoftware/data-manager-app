package life.qbic.datamanager.views.register;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.QueryParameters;
import java.util.Map;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.identity.application.user.registration.RegisterUserInput;
import life.qbic.identity.application.user.registration.RegisterUserOutput;
import life.qbic.identity.application.user.registration.UserRegistrationException;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
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

  private static final Logger log =
      LoggerFactory.logger(UserRegistrationHandler.class.getName());
  private final RegisterUserInput registrationUseCase;
  private UserRegistrationLayout userRegistrationLayout;

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
    userRegistrationLayout.email.setErrorMessage("Please provide a valid mail address");
    userRegistrationLayout.password.setHelperText("A password must be at least 8 characters");
    userRegistrationLayout.password.setPattern(".{8,}");
    userRegistrationLayout.password.setErrorMessage("Password too short");
    userRegistrationLayout.username.setHelperText("Your unique username, visible to other users");
    userRegistrationLayout.username.setErrorMessage("Please provide a username");
  }

  private void addListener() {
    userRegistrationLayout.registerButton.addClickShortcut(Key.ENTER);

    userRegistrationLayout.registerButton.addClickListener(
        event -> {
          clearNotifications();
          registrationUseCase.register(
              userRegistrationLayout.fullName.getValue(),
              userRegistrationLayout.email.getValue(),
              userRegistrationLayout.password.getValue().toCharArray(),
              userRegistrationLayout.username.getValue());
        });
  }

  private void clearNotifications() {
    userRegistrationLayout.notificationLayout.removeAll();
  }

  private void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    userRegistrationLayout.notificationLayout.add(errorMessage);
  }

  private void showAlreadyUsedEmailError() {
    showError("Email address already in use",
        "If you have difficulties with your password you can reset it.");
  }

  private void showUnexpectedError() {
    showError("Registration failed", "Please try again.");
  }

  @Override
  public void onUserRegistrationSucceeded() {
    QueryParameters registrationParams = QueryParameters.simple(Map.of("userRegistered", "true"));
    UI.getCurrent().navigate("/login", registrationParams);
  }

  @Override
  public void onUnexpectedFailure(UserRegistrationException userRegistrationException) {
    handleRegistrationFailure(userRegistrationException);
  }

  @Override
  public void onUnexpectedFailure(String reason) {
    showUnexpectedError();
  }

  private void handleRegistrationFailure(UserRegistrationException userRegistrationException) {
    if (userRegistrationException.fullNameException().isPresent()) {
      userRegistrationLayout.fullName.setInvalid(true);
    }
    if (userRegistrationException.passwordException().isPresent()) {
      userRegistrationLayout.password.setInvalid(true);
    }
    if (userRegistrationException.emailFormatException().isPresent()) {
      userRegistrationLayout.email.setInvalid(true);
    }
    if (userRegistrationException.userExistsException().isPresent()) {
      showAlreadyUsedEmailError();
    }
    if (userRegistrationException.userNameNotAvailableException().isPresent()) {
      showUserNameNotAvailableError();
    }
    if (userRegistrationException.unexpectedException().isPresent()) {
      showUnexpectedError();
    }
  }

  private void showUserNameNotAvailableError() {
    showError("Username already in use", "Please try another username");
  }
}
