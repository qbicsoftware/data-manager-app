package life.qbic.views.login.newpassword;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import life.qbic.views.components.BoxLayout;

/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
public class NewPasswordSetLayout extends BoxLayout {

  public Button loginButton;

  public NewPasswordSetLayout() {
    fillLayoutComponents();
  }

  private void fillLayoutComponents(){
    setTitleText("New password saved!");
    setDescriptionText("You can now log in with your new password.");

    loginButton = new Button("Login");
    addButtons(loginButton);

    styleButtons();
  }

  private void styleButtons(){
    loginButton.setWidthFull();
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

}
