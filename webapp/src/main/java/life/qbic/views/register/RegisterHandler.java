package life.qbic.views.register;

import com.vaadin.flow.component.UI;
import life.qbic.usermanagement.User;
import life.qbic.usermanagement.registration.RegisterUserInput;
import life.qbic.usermanagement.registration.RegisterUserOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles the {@link RegisterLayout} components
 *
 * <p>This class is responsible for enabling buttons or triggering other view relevant changes on
 * the view class components
 */
@Component
public class RegisterHandler implements RegisterHandlerInterface, RegisterUserOutput {

  private static final org.apache.logging.log4j.Logger log =
      org.apache.logging.log4j.LogManager.getLogger(RegisterHandler.class);
  private RegisterLayout userRegistrationLayout;

  private final RegisterUserInput registerUserInput;

  @Autowired
  RegisterHandler(RegisterUserInput registerUserInput) {
    this.registerUserInput = registerUserInput;
    registerUserInput.setOutput(this);
  }

  @Override
  public boolean register(RegisterLayout registerLayout) {
    if (userRegistrationLayout != registerLayout) {
      this.userRegistrationLayout = registerLayout;
      // orchestrate view
      addListener();
      // then return
      return true;
    }
    return false;
  }

  private void addListener() {
    userRegistrationLayout.registerButton.addClickListener( e -> {
      var user = User.create(
          userRegistrationLayout.password.getValue(),
          userRegistrationLayout.fullName.getValue(),
          userRegistrationLayout.email.getValue());
      registerUserInput.register(user);
    });
  }

  @Override
  public void onSuccess() {
    UI.getCurrent().navigate("/login");
  }

  @Override
  public void onFailure(String reason) {
    // Display error to the user
    // Stub output:
    System.out.println(reason);
  }
}
