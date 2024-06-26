package life.qbic.datamanager.views.login.newpassword;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.identity.application.user.password.NewPasswordInput;
import life.qbic.identity.application.user.password.NewPasswordOutput;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>Handles setting new passwords</b>
 *
 * <p>When a new password is set the handler triggers the use case to update the users password</p>
 *
 * @since 1.0.0
 */
@Component
public class NewPasswordHandler implements NewPasswordHandlerInterface, NewPasswordOutput {

  private String currentUserId;

  private NewPasswordLayout newPasswordLayout;
  private final NewPasswordInput passwordReset;

  private final String passwordResetQueryParameter;

  @Autowired
  NewPasswordHandler(NewPasswordInput newPasswordUseCase,
      @Value("${routing.password-reset.reset-parameter}") String newPasswordParam) {
    this.passwordReset = newPasswordUseCase;
    this.currentUserId = "";
    this.passwordResetQueryParameter = newPasswordParam;
  }

  @Override
  public void handle(NewPasswordLayout layout) {
    if (newPasswordLayout != layout) {
      this.newPasswordLayout = layout;
      addClickListeners();
    }
  }

  @Override
  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> params = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    var resetParam = params.keySet().stream()
        .filter(entry -> Objects.equals(
            entry, passwordResetQueryParameter)).findAny();
    if (resetParam.isPresent()) {
      currentUserId = params.get(passwordResetQueryParameter).get(0);
    } else {
      throw new NotImplementedException();
    }
  }

  private void addClickListeners() {
    newPasswordLayout.sendButton().addClickListener(buttonClickEvent ->
        passwordReset.setNewUserPassword(currentUserId,
            newPasswordLayout.newPassword().getValue().toCharArray()));
    newPasswordLayout.sendButton().addClickShortcut(Key.ENTER);

    newPasswordLayout.newPasswordSetLayout().loginButton().addClickListener(
        buttonClickEvent ->
            newPasswordLayout.newPasswordSetLayout().getUI()
                .ifPresent(ui -> ui.navigate("login")));
  }

  @Override
  public void onSuccessfulNewPassword() {
    newPasswordLayout.provideNewPasswordLayout().setVisible(false);
    newPasswordLayout.newPasswordSetLayout().setVisible(true);
  }

  @Override
  public void onPasswordValidationFailure() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void onUnexpectedFailure() {
    throw new UnsupportedOperationException();
  }
}
