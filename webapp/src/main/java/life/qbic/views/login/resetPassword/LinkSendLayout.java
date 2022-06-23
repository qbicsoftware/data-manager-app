package life.qbic.views.login.resetPassword;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.components.BoxForm;
import life.qbic.views.landing.LandingPageLayout;

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

  public LinkSendLayout() {

    initLayout();
  }

  private void initLayout(){
    BoxForm boxForm = new BoxForm();

    boxForm.setTitleText("Email has been sent!");
    boxForm.setDescriptionText("Please check your inbox and click the received link to reset your password");

    loginButton = new Button("Login");
    boxForm.addButtons(loginButton);

    createSpan();
    boxForm.addLinkSpanContent(resendSpan);

    styleButtons();

    add(boxForm);
  }

  private void createSpan() {
    Text spanText = new Text("Didn't receive the link? ");
    resendButton = new Button("Resend");
    resendSpan = new Span(spanText,resendButton);
  }

  private void styleButtons(){
    loginButton.setWidthFull();
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    resendButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
  }

}
