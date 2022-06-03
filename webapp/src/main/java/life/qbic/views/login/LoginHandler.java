package life.qbic.views.login;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import life.qbic.domain.usermanagement.login.LoginUserInput;
import life.qbic.domain.usermanagement.login.LoginUserOutput;
import life.qbic.domain.usermanagement.registration.ConfirmEmailInput;
import life.qbic.domain.usermanagement.registration.ConfirmEmailOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b> The LoginHandler handles the view elements of the {@link LoginLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class LoginHandler implements LoginHandlerInterface, ConfirmEmailOutput, LoginUserOutput {

  private LoginLayout registeredLoginView;

  private final ConfirmEmailInput confirmEmailInput;

  private final LoginUserInput loginUserInput;

  LoginHandler(@Autowired ConfirmEmailInput confirmEmailInput, LoginUserInput loginUserInput) {
    this.confirmEmailInput = confirmEmailInput;
    this.loginUserInput = loginUserInput;
    loginUserInput.setOutput(this);
  }

  @Override
  public void handle(LoginLayout loginView) {
    if (registeredLoginView != loginView) {
      registeredLoginView = loginView;
    }
    initFields();
    resetMessages();
    addListener();
  }
  private void initFields() {
    registeredLoginView.password.setHelperText("A password must be at least 8 characters");
    registeredLoginView.password.setPattern(".{8,}");
    registeredLoginView.password.setErrorMessage("Invalid Password");
    registeredLoginView.email.setErrorMessage("Invalid Email");
  }

  private void addListener() {
    registeredLoginView.loginButton.addClickShortcut(Key.ENTER);
    registeredLoginView.loginButton.addClickListener(
        event -> {
          resetMessages();
          resetComponentErrors();
          loginUserInput.login(registeredLoginView.email.getValue(),
              registeredLoginView.password.getValue());
        });
    //ToDo Add forgot password Logic
  }

  @Override
  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> queryParams = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey("error")) {
      displayUnspecificError();
    }
    if (queryParams.containsKey("confirmEmail")) {
      String userId = queryParams.get("confirmEmail").iterator().next();
      confirmEmailInput.confirmEmailAddress(userId);
    }
  }

  private void resetMessages() {
    registeredLoginView.errorMessage.setVisible(false);
    registeredLoginView.informationMessage.setVisible(false);
  }

  private void resetComponentErrors() {
    registeredLoginView.email.setInvalid(false);
    registeredLoginView.password.setInvalid(false);
  }

  @Override
  public void onEmailConfirmationSuccess() {
    displayInformation("Email address confirmed", "You can now login with your credentials.");
  }

  private void displayInformation(String title, String description) {
    resetMessages();
    registeredLoginView.informationMessage.titleTextSpan.setText(title);
    registeredLoginView.informationMessage.descriptionTextSpan.setText(description);
    registeredLoginView.informationMessage.setVisible(true);
  }

  @Override
  public void onEmailConfirmationFailure(String reason) {
    displayError("Email confirmation failed", reason);
  }

  private void displayUnspecificError() {
    displayError("Login failed", "");
  }

  private void displayError(String title, String description) {
    resetMessages();
    registeredLoginView.errorMessage.titleTextSpan.setText(title);
    registeredLoginView.errorMessage.descriptionTextSpan.setText(description);
    registeredLoginView.errorMessage.setVisible(true);
  }

  @Override
  public void onLoginSucceeded() {
    displayInformation("Login successful", "You are now logged in.");
    resetComponentErrors();
    UI.getCurrent().navigate("/register");     //TODO change to welcome page
  }

  @Override
  public void onLoginFailed() {
    registeredLoginView.email.setInvalid(true);
    registeredLoginView.password.setInvalid(true);
    displayError("Login failed", "Invalid password or email address provided.");
  }
}
