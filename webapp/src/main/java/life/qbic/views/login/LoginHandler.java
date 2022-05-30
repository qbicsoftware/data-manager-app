package life.qbic.views.login;

import com.vaadin.flow.router.BeforeEnterEvent;
import java.util.List;
import java.util.Map;
import life.qbic.domain.usermanagement.registration.ConfirmEmailInput;
import life.qbic.domain.usermanagement.registration.ConfirmEmailOutput;
import life.qbic.usermanagement.persistence.UserJpaRepository;
import life.qbic.views.ConfirmationMessage;
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
    setupView();
  }

  private void setupView() {
    registeredLoginView.confirmationMessage.setVisible(false);
  }

  @Override
  public void handle(BeforeEnterEvent beforeEnterEvent) {
    Map<String, List<String>> queryParams = beforeEnterEvent.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey("error")) {
      registeredLoginView.loginForm.setError(true);
    }
    if (queryParams.containsKey("confirmEmail")) {
      String userId = queryParams.get("confirmEmail").iterator().next();
      confirmEmailInput.confirmEmailAddress(userId);
    }
  }

  @Override
  public void onSuccess() {
    registeredLoginView.confirmationMessage = new ConfirmationMessage("Email address confirmed", "You can now login with your credentials.");
  }

  @Override
  public void onFailure(String reason) {
    // Todo Display error message
  }
}
