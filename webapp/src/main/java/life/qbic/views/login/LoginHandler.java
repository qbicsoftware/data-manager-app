package life.qbic.views.login;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.RouterLink;
import java.util.List;
import java.util.Map;
import life.qbic.domain.usermanagement.User;
import life.qbic.domain.usermanagement.registration.ConfirmEmailInput;
import life.qbic.domain.usermanagement.registration.ConfirmEmailOutput;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import life.qbic.views.helloworld.HelloWorldView;
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
    setupVisibility();
    addListener();
  }

  private void setupVisibility() {
    registeredLoginView.confirmationSuccessMessage.setVisible(false);
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
          checkUserEmail(registeredLoginView.email.getValue());
        });
    //ToDo Add forgot password Logic
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

  //ToDo this should be moved into a LoginUser Use Case
  private void checkUserEmail(String email) {
    //ToDo Check If fields are filled before parsing database
    List<User> foundUsers = userRepository.findUsersByEmail(email);
    if (!foundUsers.isEmpty()) {
      checkUserPassword(foundUsers.get(0));
    } else {
      onEmailConfirmationFailure("Invalid Credentials");
      registeredLoginView.email.setInvalid(true);
      registeredLoginView.password.setInvalid(true);
    }
  }

  //ToDo This should be moved into a LoginUser Use Case
  private void checkUserPassword(User user) {
    if (user.checkPassword(registeredLoginView.password.getValue().toCharArray())) {
      RouterLink routerLink = new RouterLink("HelloWorld", HelloWorldView.class);
      //ToDo Move User to HelloWorldView after login
    } else {
      onEmailConfirmationFailure("Invalid Credentials");
      registeredLoginView.email.setInvalid(true);
      registeredLoginView.password.setInvalid(true);
    }
  }

  private void resetMessages() {
    registeredLoginView.errorMessage.setVisible(false);
    registeredLoginView.confirmationSuccessMessage.setVisible(false);
  }

  private void resetComponentErrors() {
    registeredLoginView.email.setInvalid(false);
    registeredLoginView.password.setInvalid(false);
  }

  @Override
  public void onEmailConfirmationSuccess() {
    resetMessages();
    registeredLoginView.confirmationSuccessMessage.titleTextSpan.setText("Email address confirmed");
    registeredLoginView.confirmationSuccessMessage.descriptionTextSpan.setText("You can now login with your credentials.");
    registeredLoginView.confirmationSuccessMessage.setVisible(true);
  }

  @Override
  public void onEmailConfirmationFailure(String reason) {
    resetMessages();
    registeredLoginView.errorMessage.titleTextSpan.setText("Email confirmation failed");
    registeredLoginView.errorMessage.descriptionTextSpan.setText(reason);
    registeredLoginView.errorMessage.setVisible(true);
  }
}
