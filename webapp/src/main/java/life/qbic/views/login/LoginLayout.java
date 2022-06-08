package life.qbic.views.login;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.AbstractLogin.ForgotPasswordEvent;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.views.landing.LandingPageLayout;
import life.qbic.views.login.ConfigurableLoginForm.Message;
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
@AnonymousAllowed
public class LoginLayout extends VerticalLayout implements HasUrlParameter<String> {

  private VerticalLayout contentLayout;

  private ConfigurableLoginForm loginForm;

  private Span registerSpan;

  private final transient LoginHandlerInterface viewHandler;

  public LoginLayout(@Autowired LoginHandlerInterface loginHandlerInterface) {
    this.addClassName("grid");

    initLayout();
    styleLayout();
    viewHandler = loginHandlerInterface;
    registerToHandler(viewHandler);
  }

  private void initLayout() {
    contentLayout = new VerticalLayout();
    this.loginForm = new ConfigurableLoginForm();
    loginForm.setAction("login");

    Span registerSpan = initRegisterSpan();
    this.registerSpan = registerSpan;

    contentLayout.add(loginForm, registerSpan);

    add(contentLayout);
  }

  private void registerToHandler(LoginHandlerInterface loginHandler) {
    loginHandler.handle(this);
  }

  private void styleLayout() {
    styleFormLayout();
    setSizeFull();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
    loginForm.setUsernameText("Email");
  }

  private void styleFormLayout() {
    registerSpan.addClassName("p-l");
    contentLayout.setPadding(false);
    contentLayout.setSpacing(false);
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
  }

  private Span initRegisterSpan() {
    RouterLink routerLink = new RouterLink("REGISTER", UserRegistrationLayout.class);
    return new Span(new Text("Need an account? "), routerLink);
  }

  public void hideError() {
    loginForm.hideError();
  }

  public void showError(Message errorMessage) {
    loginForm.setError(errorMessage);
    loginForm.showError();
  }

  public void addLoginListener(ComponentEventListener<LoginEvent> loginListener) {
    loginForm.addLoginListener(loginListener);
  }

  public void addForgotPasswordListener(ComponentEventListener<ForgotPasswordEvent> listener) {
    loginForm.addForgotPasswordListener(listener);
  }

  @Override
  public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
    viewHandler.handle(event);
  }

  public void showInformation(Message message) {
    loginForm.showInformation(message);
  }

  public void resetInformation() {
    loginForm.resetInformation();
  }
}
