package life.qbic.views.login;

import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import life.qbic.domain.usermanagement.registration.ConfirmEmailInput;
import life.qbic.domain.usermanagement.registration.ConfirmEmailOutput;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import life.qbic.views.login.ConfigurableLoginForm.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b> The LoginHandler handles the view elements of the {@link LoginLayout}. </b>
 *
 * @since 1.0.0
 */
@Component
public class LoginHandler implements LoginHandlerInterface, ConfirmEmailOutput {

  private final UserJpaRepository userRepository;

  private LoginLayout registeredLoginView;

  private final ConfirmEmailInput confirmEmailInput;

  LoginHandler(@Autowired UserJpaRepository repository,
      @Autowired ConfirmEmailInput confirmEmailInput) {
    this.userRepository = repository;
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
  }

  private void resetErrorMessages() {
    setDefaultError();
    hideError();
  }

  private void hideError() {
    registeredLoginView.hideError();
  }

  private void setDefaultError() {
    registeredLoginView.showError(new ErrorMessage(
        "Incorrect username or password",
        "Check that you have entered the correct username and password and try again."
    ));
  }

  private void addListener() {
    registeredLoginView.addLoginListener(it -> onLoginSucceeded());
    //ToDo Add forgot password Logic
  }

  private void onLoginSucceeded() {
    resetErrorMessages();
  }

  @Override
  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> queryParams = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey("error")) {
      //Todo Replace this with a distinct error message in the loginView
      onEmailConfirmationFailure("The provided information was invalid");
    }
    if (queryParams.containsKey("confirmEmail")) {
      String userId = queryParams.get("confirmEmail").iterator().next();
      confirmEmailInput.confirmEmailAddress(userId);
    }
  }

  @Override
  public void onEmailConfirmationSuccess() {
    resetErrorMessages();
  }

  @Override
  public void onEmailConfirmationFailure(String reason) {
    registeredLoginView.showError(new ErrorMessage("Email confirmation failed", reason));
  }
}
