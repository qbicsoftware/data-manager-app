package life.qbic.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import life.qbic.domain.usermanagement.registration.ConfirmEmailInput;
import life.qbic.domain.usermanagement.registration.ConfirmEmailOutput;
import life.qbic.views.login.ConfigurableLoginForm.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b> The LoginHandler handles the view elements of the {@link LoginLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class LoginHandler implements LoginHandlerInterface, ConfirmEmailOutput {

  private LoginLayout registeredLoginView;

  private final ConfirmEmailInput confirmEmailInput;
  private static final Message INCORRECT_USERNAME_OR_PASSWORD = new Message(
      "Incorrect username or password",
      "Check that you have entered the correct username and password and try again."
  );

  private static final Message EMAIL_CONFIRMATION_SUCCESS = new Message(
      "Email address confirmed",
      "You can now login with your credentials."
  );

  LoginHandler(@Autowired ConfirmEmailInput confirmEmailInput) {
    this.confirmEmailInput = confirmEmailInput;
  }

  @Override
  public void handle(LoginLayout loginView) {
    if (registeredLoginView != loginView) {
      registeredLoginView = loginView;
    }
    initFields();
    addListener();
  }

  private void initFields() {
    resetErrorMessages();
    resetInformationMessages();
  }

  private void resetErrorMessages() {
    showDefaultError();
    hideError();
  }

  private void resetInformationMessages() {
    registeredLoginView.resetInformation();
  }

  private void hideError() {
    registeredLoginView.hideError();
  }

  private void showDefaultError() {
    registeredLoginView.showError(INCORRECT_USERNAME_OR_PASSWORD);
  }

  private void showEmailConfirmationInformation() {
    registeredLoginView.showInformation(EMAIL_CONFIRMATION_SUCCESS);
  }

  private void addListener() {
    registeredLoginView.addLoginListener(it -> onLoginSucceeded());
    registeredLoginView.addForgotPasswordListener(it -> {/*TODO*/});
  }

  private void onLoginSucceeded() {
    resetErrorMessages();
    resetInformationMessages();
    UI.getCurrent().navigate("/hello");
  }

  @Override
  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> queryParams = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey("error")) {
      //Todo Replace this with a distinct error message in the loginView
      showDefaultError();
    }
    if (queryParams.containsKey("confirmEmail")) {
      String userId = queryParams.get("confirmEmail").iterator().next();
      confirmEmailInput.confirmEmailAddress(userId);
    }
  }

  @Override
  public void onEmailConfirmationSuccess() {
    resetErrorMessages();
    showEmailConfirmationInformation();

  }

  @Override
  public void onEmailConfirmationFailure(String reason) {
    registeredLoginView.showError(new Message("Email confirmation failed", reason));
  }
}
