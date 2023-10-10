package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import life.qbic.datamanager.views.layouts.BoxLayout;

/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
public class LinkSentLayout extends BoxLayout {

  public Button loginButton;

  public LinkSentLayout() {
    fillLayoutComponents();
  }

  private void fillLayoutComponents() {
    setTitleText("Email has been sent!");
    setDescriptionText(
        "Please check your inbox and follow the instructions to reset your password.");

    loginButton = new Button("Login");
    addButtons(loginButton);

    styleButtons();
  }

  private void styleButtons() {
    loginButton.setWidthFull();
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

}
