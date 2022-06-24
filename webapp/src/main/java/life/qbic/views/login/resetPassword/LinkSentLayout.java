package life.qbic.views.login.resetPassword;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.components.BoxLayout;
import life.qbic.views.landing.LandingPageLayout;

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
