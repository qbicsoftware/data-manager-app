package life.qbic.datamanager.views.login.passwordreset;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.datamanager.views.components.BoxLayout;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationLayout;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Reset Password")
@Route(value = "reset-password", layout = LandingPageLayout.class)
@AnonymousAllowed
public class ResetPasswordLayout extends VerticalLayout{

  public EmailField email;
  public Button sendButton;
  public Span registerSpan;
  public BoxLayout enterEmailLayout;
  public LinkSentLayout linkSentLayout;

  public ResetPasswordLayout(@Autowired PasswordResetHandlerInterface passwordResetHandler) {
    initLayout();
    styleLayout();
    registerToHandler(passwordResetHandler);
  }

  private void registerToHandler(PasswordResetHandlerInterface passwordResetHandler) {
    passwordResetHandler.handle(this);
  }

  private void initLayout() {
    initLinkSentLayout();

    initEnterEmailLayout();

    add(enterEmailLayout,linkSentLayout);
  }

  private void initEnterEmailLayout() {
    enterEmailLayout = new BoxLayout();

    enterEmailLayout.setTitleText("Reset password");
    enterEmailLayout.setDescriptionText("Enter the mail address associated with your account and we'll send you a link to reset your password:");

    email = new EmailField("Email");
    enterEmailLayout.addFields(email);

    createSendButton();
    enterEmailLayout.addButtons(sendButton);

    createSpan();
    enterEmailLayout.addLinkSpanContent(registerSpan);
  }

  private void initLinkSentLayout() {
    linkSentLayout = new LinkSentLayout();
    linkSentLayout.setVisible(false);
  }

  private void styleLayout() {

    styleFieldLayout();
    styleSendButton();
    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void createSpan() {
    RouterLink link = new RouterLink("REGISTER", UserRegistrationLayout.class);
    registerSpan = new Span(new Text("Need an account? "), link);
  }

  private void createSendButton() {
    sendButton = new Button("Send");
  }

  private void styleFieldLayout() {
    email.setWidthFull();
  }

  private void styleSendButton() {
    sendButton.setWidthFull();
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }
}
