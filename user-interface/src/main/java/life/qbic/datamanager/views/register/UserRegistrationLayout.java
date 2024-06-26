package life.qbic.datamanager.views.register;

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
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.stream.Stream;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.datamanager.views.login.passwordreset.ResetPasswordLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b> Defines the look of the registration layout. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Register")
@Route(value = AppRoutes.REGISTER, layout = LandingPageLayout.class)
@CssImport("./styles/views/login/login-view.css")
@AnonymousAllowed
@UIScope
public class UserRegistrationLayout extends VerticalLayout {

  @Serial
  private static final long serialVersionUID = 6995209728843801219L;

  public EmailField email;

  public PasswordField password;

  public TextField fullName;

  public TextField username;

  public Button registerButton;

  private Span loginSpan;
  private Span resetSpan;

  public VerticalLayout notificationLayout;
  private VerticalLayout fieldLayout;
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
    createNotificationLayout();
    createFieldLayout();
    createRegisterButton();
    createSpans();
    add(contentLayout);
    contentLayout.add(layoutTitle, notificationLayout, fieldLayout, registerButton, loginSpan, resetSpan);
  }

  private void styleLayout() {
    styleFieldLayout();
    styleNotificationLayout();
    styleRegisterButton();
    styleFormLayout();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void createNotificationLayout() {
    notificationLayout = new VerticalLayout();
  }

  private void createFieldLayout() {
    fieldLayout = new VerticalLayout();
    createEmailField();
    createNameField();
    createUsernameField();
    createPasswordField();
    fieldLayout.add(fullName, email, username, password);
  }

  private void createUsernameField() {
    username = new TextField("Username");
    username.setMinLength(1);
    username.setPattern(".*\\S+.*"); //requires to contain at least one non-whitespace character
  }

  private void createSpans() {
    RouterLink link = new RouterLink("LOGIN", LoginLayout.class);
    loginSpan = new Span(new Text("Already have an account? "), link);

    RouterLink resetLink = new RouterLink("RESET", ResetPasswordLayout.class);
    resetSpan = new Span(new Text("Forgot your password? "), resetLink);
  }

  private void createRegisterButton() {
    registerButton = new Button("Register");
  }

  private void createNameField() {
    fullName = new TextField("Full Name");
  }

  private void createEmailField() {
    email = new EmailField("Email");
  }

  private void createPasswordField() {
    password = new PasswordField("Password");
  }

  private void styleNotificationLayout() {
    notificationLayout.setPadding(false);
  }

  private void styleFormLayout() {
    contentLayout.setPadding(false);
    contentLayout.setMargin(false);
    contentLayout.addClassNames("bg-base", "border", "rounded-m", "border-contrast-10",
        "box-border", "flex", "flex-col", "w-full", "text-s", "shadow-l", "min-width-300px",
        "max-width-15vw", "pb-l", "pr-l", "pl-l");
  }

  private void styleFieldLayout() {
    password.setWidthFull();
    email.setWidthFull();
    fullName.setWidthFull();
    username.setWidthFull();
    setRequiredIndicatorVisible(fullName, email, username, password);
    fieldLayout.setSpacing(false);
    fieldLayout.setMargin(false);
    fieldLayout.setPadding(false);
  }

  private void styleRegisterButton() {
    registerButton.setWidthFull();
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
  }

  private void setRequiredIndicatorVisible(HasValueAndElement<?, ?>... components) {
    Stream.of(components).forEach(comp -> comp.setRequiredIndicatorVisible(true));
  }
}
