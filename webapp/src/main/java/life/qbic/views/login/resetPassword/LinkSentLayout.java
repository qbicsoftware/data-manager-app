package life.qbic.views.login.resetPassword;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import life.qbic.views.components.BoxLayout;

/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
public class LinkSentLayout extends BoxLayout {

  public Button loginButton;

  public LinkSentLayout() {
    initLayout();
  }

  private void initLayout(){
    setTitleText("Email has been sent!");
    setDescriptionText("Please check your inbox and click the received link to reset your password");

    loginButton = new Button("Login");
    addButtons(loginButton);

    styleButtons();
  }

  private void styleButtons(){
    loginButton.setWidthFull();
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

}
