package life.qbic.views.login;

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
import com.vaadin.flow.router.*;
import java.util.stream.Stream;
import life.qbic.views.SuccessMessage;
import life.qbic.views.ErrorMessage;
import life.qbic.views.landing.LandingPageLayout;
import life.qbic.views.register.UserRegistrationLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Defines the layout and look of the login view. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Login")
@Route(value = "login", layout = LandingPageLayout.class)
@CssImport("./styles/views/login/login-view.css")
public class LoginLayout extends VerticalLayout implements BeforeEnterObserver {

  private VerticalLayout contentLayout;

  public PasswordField password;

  public EmailField email;

  public Button loginButton;

  public Button forgotPasswordButton;
  public Span registerSpan;

  private H2 layoutTitle;
  private LoginHandlerInterface loginHandlerInterface;

  public SuccessMessage confirmationSuccessMessage;
  public ErrorMessage errorMessage;

  public ErrorMessage invalidEmailMessage;

  public ErrorMessage passwordTooShortMessage;

  public ErrorMessage unConfirmedEmailMessage;

  public LoginLayout(@Autowired LoginHandlerInterface loginHandlerInterface) {
    this.addClassName("grid");
    initLayout();
    styleLayout();
    registerToHandler(loginHandlerInterface);
  }

  private void initLayout() {
    contentLayout = new VerticalLayout();
    createComponents();
    styleComponents();
    add(contentLayout);
  }

  private void registerToHandler(LoginHandlerInterface loginHandler) {
    this.loginHandlerInterface = loginHandler;
    loginHandler.handle(this);
  }

  private void styleLayout() {
    styleFormLayout();
    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void createComponents() {
    layoutTitle = new H2("Log In");
    createEmailField();
    createPasswordField();
    createLoginButton();
    createRegisterSpan();
    createForgotPasswordButton();
    createDivs();
  }

  private void styleComponents() {
    password.setWidthFull();
    email.setWidthFull();
    styleLoginButton();
    setRequiredIndicatorVisible(email, password);
    styleForgotPasswordButton();
  }

  private void createDivs() {
    createErrorDivs();
    createSuccessDivs();
  }

  private void createErrorDivs() {
    errorMessage = new ErrorMessage("Log In failed", "Please try again.");
    errorMessage.setVisible(false);
    invalidEmailMessage = new ErrorMessage("Invalid credentials",
        "The provided email or password is invalid");
    invalidEmailMessage.setVisible(false);
    passwordTooShortMessage = new ErrorMessage("Password too short",
        "Your password must be at least 8 characters long.");
    passwordTooShortMessage.setVisible(false);
    unConfirmedEmailMessage = new ErrorMessage("Unconfirmed email address",
        "Please confirm your email address before logging in");
    unConfirmedEmailMessage.setVisible(false);
  }

  private void createSuccessDivs() {
    confirmationSuccessMessage = new SuccessMessage("What a success!",
        "I don't know what you want to show here");
    confirmationSuccessMessage.setVisible(false);
  }

  private void styleFormLayout() {
    contentLayout.addClassNames(
        "bg-base",
        "border",
        "border-contrast-10",
        "rounded-m",
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
        confirmationSuccessMessage,
        email,
        password,
        loginButton,
        forgotPasswordButton,
        registerSpan);
  }

  private void createEmailField() {
    email = new EmailField("Email");
  }

  private void createPasswordField() {
    password = new PasswordField("Password");
  }

  private void createLoginButton() {
    loginButton = new Button("Log In");
  }

  private void styleLoginButton() {
    loginButton.setWidthFull();
    loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  private void createForgotPasswordButton() {
    forgotPasswordButton = new Button("Forgot Password");
  }

  private void styleForgotPasswordButton() {
    forgotPasswordButton.setWidthFull();
    forgotPasswordButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
  }

  private void createRegisterSpan() {
    RouterLink routerLink = new RouterLink("REGISTER", UserRegistrationLayout.class);
    registerSpan = new Span(new Text("Need an account? "), routerLink);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    loginHandlerInterface.handle(beforeEnterEvent);
  }

  private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
    Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
  }

}
