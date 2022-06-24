package life.qbic.views.login;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import life.qbic.identityaccess.application.user.ConfirmEmailInput;
import life.qbic.identityaccess.application.user.ConfirmEmailOutput;
import life.qbic.views.components.ErrorMessage;
import life.qbic.views.components.InformationMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

  private final String emailConfirmationParameter;

  @Autowired
  LoginHandler(ConfirmEmailInput confirmEmailInput,
      @Value("${EMAIL_CONFIRMATION_PARAMETER:confirm-email}") String emailConfirmationParameter) {
    this.confirmEmailInput = confirmEmailInput;
    this.emailConfirmationParameter = emailConfirmationParameter;
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
    clearNotifications();
  }

  private void showInvalidCredentialsError() {
    showError(new ErrorMessage("Incorrect username or password", "Please try again."));
  }

  private void showEmailConfirmationInformation() {
    showInformation(new InformationMessage("Email address confirmed",
        "You can now login with your credentials."));
  }

  private void showEmailConfirmationReminder() {
    showInformation(new InformationMessage("Registration email sent",
        "Please check your email inbox to confirm your registration"));
  }

  public void clearNotifications() {
    registeredLoginView.notificationLayout.removeAll();
  }

  public void showError(ErrorMessage errorMessage) {
    clearNotifications();
    registeredLoginView.notificationLayout.add(errorMessage);
  }

  public void showInformation(InformationMessage message) {
    clearNotifications();
    registeredLoginView.notificationLayout.add(message);
  }

  private void addListener() {
    registeredLoginView.addLoginListener(it -> onLoginSucceeded());
    registeredLoginView.addForgotPasswordListener(it -> {/*TODO*/});
  }

  private void onLoginSucceeded() {
    clearNotifications();
    UI.getCurrent().navigate("/hello");
  }

  @Override
  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> queryParams = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey("error")) {
      showInvalidCredentialsError();
    }
    if (queryParams.containsKey(emailConfirmationParameter)) {
      String userId = queryParams.get(emailConfirmationParameter).iterator().next();
      confirmEmailInput.confirmEmailAddress(userId);
    }
    if (queryParams.containsKey("userRegistered")) {
      showEmailConfirmationReminder();
    }
  }

  @Override
  public void onEmailConfirmationSuccess() {
    showEmailConfirmationInformation();
  }

  @Override
  public void onEmailConfirmationFailure(String reason) {
    showError(new ErrorMessage("Email confirmation failed", reason));
  }
}
