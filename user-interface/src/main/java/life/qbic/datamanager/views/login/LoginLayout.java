package life.qbic.datamanager.views.login;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationLayout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Defines the layout and look of the login view. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Login")
@Route(value = AppRoutes.LOGIN, layout = LandingPageLayout.class)
@CssImport("./styles/views/login/login-view.css")
@AnonymousAllowed
public class LoginLayout extends VerticalLayout implements HasUrlParameter<String> {

  private VerticalLayout contentLayout;

  public VerticalLayout notificationLayout;
  private H2 title;

  private ConfigurableLoginForm loginForm;

  private Div registrationSection;

  private final transient LoginHandlerInterface viewHandler;

  public LoginLayout(@Autowired LoginHandlerInterface loginHandlerInterface) {
    initLayout();
    styleLayout();
    viewHandler = loginHandlerInterface;
    registerToHandler(viewHandler);
  }

  private void initLayout() {
    contentLayout = new VerticalLayout();
    createNotificationLayout();
    createLoginForm();
    this.registrationSection = initRegistrationSection();
    title = new H2("Log in");
    contentLayout.add(title, notificationLayout, loginForm, registrationSection);
    //TODO make relative to root including context path
    Anchor orcidOauth = new Anchor("/dev/oauth2/authorization/orcid", "Login with ORCID");
    orcidOauth.setRouterIgnore(true);
    contentLayout.add(orcidOauth);
    add(contentLayout);

  }

  private void registerToHandler(LoginHandlerInterface loginHandler) {
    loginHandler.handle(this);
  }

  private void styleLayout() {
    styleNotificationLayout();
    styleFormLayout();
    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
  }

  private void styleFormLayout() {
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
        "max-width-15vw",
        "pb-l",
        "pr-l",
        "pl-l");
  }

  private void createNotificationLayout() {
    notificationLayout = new VerticalLayout();
  }

  private void createLoginForm() {
    this.loginForm = new ConfigurableLoginForm();
    loginForm.setAction("login");
    loginForm.setUsernameText("Email");
  }

  private Div initRegistrationSection() {
    RouterLink routerLink = new RouterLink("REGISTER", UserRegistrationLayout.class);
    return new Div(new Text("Need an account? "), routerLink);
  }

  private void styleNotificationLayout() {
    notificationLayout.setPadding(false);
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
}
