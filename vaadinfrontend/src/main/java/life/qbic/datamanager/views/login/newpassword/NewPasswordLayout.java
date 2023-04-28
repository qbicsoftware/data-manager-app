package life.qbic.datamanager.views.login.newpassword;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.layouts.BoxLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serial;


/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("New Password")
@Route(value = AppRoutes.NEW_PASSWORD, layout = LandingPageLayout.class)
@AnonymousAllowed
public class NewPasswordLayout extends VerticalLayout implements HasUrlParameter<String> {

  @Serial
  private static final long serialVersionUID = 4884878964166607894L;
  private PasswordField newPassword;
  private Button sendButton;

  private BoxLayout provideNewPasswordLayout;
  private NewPasswordSetLayout newPasswordSetLayout;

  private transient NewPasswordHandlerInterface handlerInterface;

  public NewPasswordLayout(@Autowired NewPasswordHandlerInterface passwordResetHandler) {
    initLayout();
    styleLayout();
    registerToHandler(passwordResetHandler);
  }

  public PasswordField newPassword() {
    return newPassword;
  }

  public Button sendButton() {
    return sendButton;
  }

  public BoxLayout provideNewPasswordLayout() {
    return provideNewPasswordLayout;
  }

  public NewPasswordSetLayout newPasswordSetLayout() {
    return newPasswordSetLayout;
  }

  public NewPasswordHandlerInterface handlerInterface() {
    return handlerInterface;
  }

  private void registerToHandler(NewPasswordHandlerInterface passwordResetHandler) {
    passwordResetHandler.handle(this);
    handlerInterface = passwordResetHandler;
  }

  private void initLayout() {
    initPasswordSetLayout();

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

  }

  private void initPasswordSetLayout() {
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
  public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String parameter) {
    handlerInterface.handle(beforeEvent);
  }
}
