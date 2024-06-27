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
public class NewPasswordHandler {

  private final String passwordResetQueryParameter;

  @Autowired
  NewPasswordHandler(@Value("${password-reset-parameter}") String newPasswordParam) {
    this.passwordResetQueryParameter = newPasswordParam;
  }

  public String passwordResetQueryParameter() {
    return passwordResetQueryParameter;
  }

}
