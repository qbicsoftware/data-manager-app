package life.qbic.views.login.newpassword;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.router.BeforeEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.identityaccess.application.user.NewPasswordInput;
import life.qbic.identityaccess.application.user.NewPasswordOutput;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * <b>Handles the password reset</b>
 *
 * <p>When a password reset is triggered the handler starts the use case. On success the view is
 * toggled and the user can login again. On failure the user sees an error notification</p>
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
      @Value("${password-reset-parameter}") String newPasswordParam) {
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
     // unknown query
     // TODO show error
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
