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
import life.qbic.views.landing.LandingPageLayout;
import life.qbic.views.login.LoginLayout;
import org.springframework.beans.factory.annotation.Autowired;

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
    notificationLayout = new VerticalLayout();
    fieldLayout = new VerticalLayout();
    createEmailField();
    createNameField();
    createPasswordField();
    createRegisterButton();
    createSpan();

    add(contentLayout);
  }

  private void styleLayout() {

    styleFieldLayout();
    styleRegisterButton();
    styleFormLayout();
    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void createSpan() {
    RouterLink link = new RouterLink("LOGIN", LoginLayout.class);
    loginSpan = new Span(new Text("Already have an account? "), link);
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

  private void styleFormLayout() {
    contentLayout.setPadding(false);
    contentLayout.setMargin(false);
    contentLayout.addClassNames("bg-base", "border", "rounded-m", "border-contrast-10",
        "box-border", "flex", "flex-col", "w-full", "text-s", "shadow-l", "min-width-300px",
        "max-width-15vw", "pb-l", "pr-l", "pl-l");
    contentLayout.add(layoutTitle, notificationLayout, fieldLayout, registerButton, loginSpan);
  }

  private void styleFieldLayout() {
    fieldLayout.add(fullName, email, password);
    password.setWidthFull();
    email.setWidthFull();
    fullName.setWidthFull();
    setRequiredIndicatorVisible(fullName, email, password);
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
