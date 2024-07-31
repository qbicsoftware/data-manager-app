package life.qbic.datamanager.views.login;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.AbstractLogin.ForgotPasswordEvent;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.login.LoginI18n.ErrorMessage;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.general.oidc.OidcLogo;
import life.qbic.datamanager.views.general.oidc.OidcType;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.register.UserRegistrationMain;
import life.qbic.identity.api.UserInformationService;
import org.springframework.beans.factory.annotation.Value;

/**
 * User Login Component
 * <p>
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component, Providing the input fields with validation during user login
 */
@SpringComponent
@UIScope
public class UserLoginComponent extends Div {

  @Serial
  private static final long serialVersionUID = 252147637041614288L;
  private final H2 titleSpan = new H2("Log in");
  private final Div notificationLayout = new Div();
  private final ConfigurableLoginForm loginForm;
  private final LoginI18n loginI18n = new LoginI18n();
  private final Div registrationSection;
  private final transient UserInformationService userInformationService;

  public UserLoginComponent(UserInformationService userInformationService,
      @Value("${server.servlet.context-path}") String contextPath) {
    this.userInformationService = Objects.requireNonNull(userInformationService);
    addClassName("user-login-component");
    this.loginForm = createLoginForm();
    this.registrationSection = createRegistrationSection(contextPath);
    add(titleSpan, notificationLayout, loginForm, registrationSection);
  }

  private ConfigurableLoginForm createLoginForm() {
    ConfigurableLoginForm loginForm = new ConfigurableLoginForm();
    loginForm.setAction("login");
    loginForm.setI18n(loginI18n);
    loginForm.setUsernameText("Email");
    return loginForm;
  }

  private Div createRegistrationSection(String contextPath) {
    RouterLink routerLink = new RouterLink("Register", UserRegistrationMain.class);
    Span registrationLink = new Span(new Text("Don't have an account? "), routerLink);
    registrationLink.addClassName("registration-link");
    Span spacer = new Span("OR");
    spacer.addClassName("spacer");
    OidcLogo oidcLogo = new OidcLogo(OidcType.ORCID);
    LoginCard orcidCard = new LoginCard(oidcLogo, "Login with ORCiD",
        contextPath + "/oauth2/authorization/orcid");
    Div registrationSection = new Div(registrationLink, spacer, orcidCard);
    registrationSection.addClassName("registration-section");
    return registrationSection;
  }


  private void clearNotifications() {
    notificationLayout.removeAll();
    loginI18n.setErrorMessage(new ErrorMessage());
    loginForm.setError(false);
  }

  public void showError(String title, String description) {
    ErrorMessage errorMessage = new ErrorMessage();
    errorMessage.setTitle(title);
    errorMessage.setMessage(description);
    loginI18n.setErrorMessage(errorMessage);
    loginForm.setI18n(loginI18n);
    loginForm.setError(true);
  }

  public void addLoginListener(ComponentEventListener<AbstractLogin.LoginEvent> listener) {
    loginForm.addLoginListener(listener);
  }

  public void addForgotPasswordListener(ComponentEventListener<ForgotPasswordEvent> listener) {
    loginForm.addForgotPasswordListener(listener);
  }

  void showInvalidCredentialsError() {
    showError("Incorrect username or password", "Please try again.");
  }

  void showEmailConfirmationInformation() {
    showInformation("Email address confirmed", "You can now login with your credentials.");
  }

  void showEmailConfirmationReminder() {
    showInformation("Registration mail sent",
        "Please check your mail inbox to confirm your registration");
  }

  public void showInformation(String title, String description) {
    clearNotifications();
    InformationMessage informationMessage = new InformationMessage(title, description);
    notificationLayout.add(informationMessage);
  }

  void onEmailConfirmationFailure() {
    showError("Email confirmation failed",
        "Unknown user for request. If the issue persists, please contact our helpdesk.");
  }

  private static class LoginCard extends Span {

    private final Span text = new Span();

    public LoginCard(Image logo, String description, String url) {
      text.setText(description);
      text.addClassName("text");
      add(logo, text);
      addClassName("login-card");
      addClickListener(event -> UI.getCurrent().getPage().open(url, "_self"));
    }
  }
}
