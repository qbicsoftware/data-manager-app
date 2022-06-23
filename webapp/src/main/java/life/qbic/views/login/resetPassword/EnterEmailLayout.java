package life.qbic.views.login.resetPassword;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.landing.LandingPageLayout;
import life.qbic.views.login.LoginLayout;
import life.qbic.views.register.UserRegistrationLayout;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * <b> Defines the look of the password reset layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Account Recovery")
@Route(value = "account-recovery", layout = LandingPageLayout.class)
@CssImport("./styles/views/login/login-view.css")
@AnonymousAllowed
public class EnterEmailLayout extends VerticalLayout{

  public EmailField email;

  public Button sendButton;

  public Span loginSpan;

  private VerticalLayout fieldLayout;
  private final VerticalLayout contentLayout;
  private H2 layoutTitle;
  private Text descriptionText;

  public EnterEmailLayout(@Autowired PasswordResetHandlerInterface passwordResetHandler) {

    this.addClassName("grid");
    contentLayout = new VerticalLayout();

    initLayout();
    styleLayout();
    registerToHandler(passwordResetHandler);
  }

  private void registerToHandler(PasswordResetHandlerInterface passwordResetHandler) {
    passwordResetHandler.handle(this);
  }

  private void initLayout() {
    layoutTitle = new H2("Reset Password");

    descriptionText = new Text("Enter the email address associated with your account and we'll send you a link to reset your password");

    fieldLayout = new VerticalLayout();
    createEmailField();
    createRegisterButton();
    createSpan();

    add(contentLayout);
  }

  private void styleLayout() {

    styleFieldLayout();
    styleSendButton();
    styleFormLayout();
    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void createSpan() {
    RouterLink link = new RouterLink("REGISTER", UserRegistrationLayout.class);
    loginSpan = new Span(new Text("Need an account? "), link);
  }

  private void createRegisterButton() {
    sendButton = new Button("Send");
  }

  private void createEmailField() {
    email = new EmailField("Email");
  }

  private void styleFormLayout() {
    contentLayout.setPadding(false);
    contentLayout.setMargin(false);
    contentLayout.addClassNames(
        "bg-base",
        "border",
        "rounded-m",
        "border-contrast-10",
        "box-border",
        "flex",
        "flex-col",
        "w-full",
        "text-s",
        "shadow-l",
        "min-width-300px",
        "max-width-15vw",
        "pb-l",
        "pr-l",
        "pl-l");
    contentLayout.add(
        layoutTitle,
        descriptionText,
        fieldLayout,
        sendButton,
        loginSpan);
  }

  private void styleFieldLayout() {
    fieldLayout.add(email);
    email.setWidthFull();
    fieldLayout.setSpacing(false);
    fieldLayout.setMargin(false);
    fieldLayout.setPadding(false);
  }

  private void styleSendButton() {
    sendButton.setWidthFull();
    sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }
}
