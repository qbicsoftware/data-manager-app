package life.qbic.datamanager.views.login;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.AbstractLogin.ForgotPasswordEvent;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Objects;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.identity.api.UserInformationService;

/**
 * User Registration Component
 * <p>
 * Card Stylized component similar to {@link com.vaadin.flow.component.login.LoginOverlay}
 * component, Providing the input fields with validation during user registration
 */
@SpringComponent
@UIScope
public class UserLoginComponent extends Div {

  @Serial
  private static final long serialVersionUID = 252147637041614288L;
  private ConfigurableLoginForm loginForm;
  private final Div notificationLayout = new Div();
  private final H2 titleSpan = new H2("Log in");
  private final transient UserInformationService userInformationService;
  private static final String ORCID_LOGO_PATH = "login/orcid_logo.svg";

  public UserLoginComponent(UserInformationService userInformationService) {
    this.userInformationService = Objects.requireNonNull(userInformationService);
    addClassName("user-login-component");
    createLoginForm();
    add(titleSpan, notificationLayout, loginForm);
  }

  private void createLoginForm() {
    this.loginForm = new ConfigurableLoginForm();
    loginForm.setAction("login");
    loginForm.setUsernameText("Email");
  }

  private AbstractStreamResource getOrcIdSource() {
    return new StreamResource("orcid_logo.svg",
        () -> getClass().getClassLoader().getResourceAsStream(ORCID_LOGO_PATH));
  }

  private void clearNotifications() {
    notificationLayout.removeAll();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    notificationLayout.add(errorMessage);
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
    private final Image logo = new Image();

    public LoginCard(AbstractStreamResource imageResource, String description, String url) {
      logo.addClassName("logo");
      text.setText(description);
      text.addClassName("text");
      logo.setSrc(imageResource);
      add(logo, text);
      addClassName("login-card");
      addClickListener(event -> UI.getCurrent().getPage().open(url, "_self"));
    }
  }
}
