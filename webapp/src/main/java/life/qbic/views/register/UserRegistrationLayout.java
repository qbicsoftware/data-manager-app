package life.qbic.views.register;

import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.stream.Stream;
import life.qbic.views.components.ErrorMessage;
import life.qbic.views.components.InformationMessage;
import life.qbic.views.landing.LandingPageLayout;
import life.qbic.views.login.LoginLayout;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Stream;

/**
 * <b> Defines the look of the registration layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Register")
@Route(value = "register", layout = LandingPageLayout.class)
@CssImport("./styles/views/login/login-view.css")
@AnonymousAllowed
public class UserRegistrationLayout extends VerticalLayout {

  public EmailField email;

  public PasswordField password;

  public TextField fullName;

  public Button registerButton;

  public Span loginSpan;

  public ErrorMessage alreadyUsedEmailMessage;
  public ErrorMessage passwordTooShortMessage;
  public ErrorMessage errorMessage;

  public InformationMessage confirmationInformationMessage;

  public ErrorMessage invalidCredentialsMessage;
  private final VerticalLayout contentLayout;
  private H2 layoutTitle;

  public UserRegistrationLayout(@Autowired UserRegistrationHandlerInterface registerHandler) {

    this.addClassName("grid");
    contentLayout = new VerticalLayout();

    initLayout();
    styleLayout();
    registerToHandler(registerHandler);
  }

  private void registerToHandler(UserRegistrationHandlerInterface registerHandler) {
    registerHandler.handle(this);
  }

  private void initLayout() {
    layoutTitle = new H2("Register");

    createDivs();
    styleEmailField();
    styleNameField();
    createPasswordField();
    createRegisterButton();
    createSpan();

    add(contentLayout);
  }

  private void styleLayout() {
    password.setWidthFull();
    email.setWidthFull();
    fullName.setWidthFull();

    styleRegisterButton();

    setRequiredIndicatorVisible(fullName, email, password);

    styleFormLayout();
    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void createDivs() {
    createErrorDivs();
    createInformationDivs();
  }

  private void createErrorDivs() {
    alreadyUsedEmailMessage =
        new ErrorMessage(
            "Email already in use",
            "If you have difficulties with your password you can reset it.");
    alreadyUsedEmailMessage.setVisible(false);
    errorMessage = new ErrorMessage("Registration failed", "Please try again.");
    errorMessage.setVisible(false);
    passwordTooShortMessage =
        new ErrorMessage("EncryptedPassword too short", "Your password must be at least 8 characters long.");
    passwordTooShortMessage.setVisible(false);
    invalidCredentialsMessage = new ErrorMessage("Invalid Credentials", "Please check the provided user credentials");
    invalidCredentialsMessage.setVisible(false);
  }

  private void createInformationDivs() {
    confirmationInformationMessage = new InformationMessage("Confirmation successful",
        "You can now login with your credentials");
    confirmationInformationMessage.setVisible(false);
  }

  private void styleNameField() {
    fullName = new TextField("Full Name");
  }

  private void styleFormLayout() {
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
        "max-width-15vw");
    contentLayout.add(
        layoutTitle,
        errorMessage,
        alreadyUsedEmailMessage,
        passwordTooShortMessage,
        confirmationInformationMessage,
        invalidCredentialsMessage,
        fullName,
        email,
        password,
        registerButton,
        loginSpan);
  }

  private void createSpan() {
    RouterLink link = new RouterLink("LOGIN", LoginLayout.class);
    loginSpan = new Span(new Text("Already have an account? "), link);
  }

  private void createRegisterButton() {
    registerButton = new Button("Register");
  }

  private void styleRegisterButton() {
    registerButton.setWidthFull();
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  private void createPasswordField() {
    password = new PasswordField("EncryptedPassword");
  }

  private void styleEmailField() {
    email = new EmailField("Email");
  }

  private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
    Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
  }
}
