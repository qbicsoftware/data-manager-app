package life.qbic.views.login.resetPassword;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.components.BoxLayout;
import life.qbic.views.landing.LandingPageLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Account Recovery")
@Route(value = "account-recovery/sent", layout = LandingPageLayout.class)
@CssImport("./styles/views/login/login-view.css")
@AnonymousAllowed
public class LinkSendLayout extends VerticalLayout {

  public Button loginButton;
  private Span resendSpan;
  public Button resendButton;

  public LinkSendLayout(@Autowired LinkSendHandlerInterface linkSendHandlerInterface) {

    initLayout();
    registerLayout(linkSendHandlerInterface);
  }

  private void registerLayout(LinkSendHandlerInterface linkSendHandlerInterface) {
    linkSendHandlerInterface.handle(this);
  }

  private void initLayout(){
    BoxLayout boxLayout = new BoxLayout();

    boxLayout.setTitleText("Email has been sent!");
    boxLayout.setDescriptionText("Please check your inbox and click the received link to reset your password");

    loginButton = new Button("Login");
    boxLayout.addButtons(loginButton);

    styleButtons();

    add(boxLayout);
  }

  private void styleButtons(){
    loginButton.setWidthFull();
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resendButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
  }

}
