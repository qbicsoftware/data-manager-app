package life.qbic.views.login.newpassword;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.io.Serial;
import life.qbic.views.components.BoxLayout;
import life.qbic.views.landing.LandingPageLayout;
import life.qbic.views.register.UserRegistrationLayout;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("New Password")
@Route(value = "new-password", layout = LandingPageLayout.class)
@AnonymousAllowed
public class NewPasswordLayout extends VerticalLayout implements HasUrlParameter<String> {

  @Serial
  private static final long serialVersionUID = 4884878964166607894L;
  public PasswordField newPassword;
  public Button sendButton;
  public Span registerSpan;
  public BoxLayout provideNewPasswordLayout;
  public NewPasswordSetLayout newPasswordSetLayout;

  public NewPasswordHandlerInterface handlerInterface;

  public NewPasswordLayout(@Autowired NewPasswordHandlerInterface passwordResetHandler) {
    initLayout();
    styleLayout();
    registerToHandler(passwordResetHandler);
  }

  private void registerToHandler(NewPasswordHandlerInterface passwordResetHandler) {
    passwordResetHandler.handle(this);
    handlerInterface = passwordResetHandler;
  }

  private void initLayout() {
    initLinkSentLayout();

    initEnterEmailLayout();

    add(provideNewPasswordLayout, newPasswordSetLayout);
  }

  private void initEnterEmailLayout() {
    provideNewPasswordLayout = new BoxLayout();

    provideNewPasswordLayout.setTitleText("Set new password");
    provideNewPasswordLayout.setDescriptionText("Please provide a new password for your account:");

    newPassword = new PasswordField("Password");
    provideNewPasswordLayout.addFields(newPassword);

    createSendButton();
    provideNewPasswordLayout.addButtons(sendButton);

    createSpan();
    provideNewPasswordLayout.addLinkSpanContent(registerSpan);
  }

  private void initLinkSentLayout() {
    newPasswordSetLayout = new NewPasswordSetLayout();
    newPasswordSetLayout.setVisible(false);
  }

  private void styleLayout() {

    styleFieldLayout();
    styleSendButton();
    setSizeFull();
    setAlignItems(Alignment.CENTER);
    setJustifyContentMode(JustifyContentMode.CENTER);
  }

  private void createSpan() {
    RouterLink link = new RouterLink("REGISTER", UserRegistrationLayout.class);
    registerSpan = new Span(new Text("Need an account? "), link);
  }

  private void createSendButton() {
    sendButton = new Button("Send");
  }

  private void styleFieldLayout() {
    newPassword.setWidthFull();
  }

  private void styleSendButton() {
    sendButton.setWidthFull();
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  @Override
  public void setParameter(BeforeEvent beforeEvent, String s) {
    handlerInterface.handle(beforeEvent);
  }
}
