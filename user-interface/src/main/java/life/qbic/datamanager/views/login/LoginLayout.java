package life.qbic.datamanager.views.login;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
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
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.register.UserRegistrationMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * <b>Defines the layout and look of the login view. </b>
 *
 * @since 1.0.0
 */
@PageTitle("Login")
@Route(value = AppRoutes.LOGIN, layout = LandingPageLayout.class)
@CssImport("./styles/views/login/login-view.css")
@AnonymousAllowed
@SpringComponent
@UIScope
public class LoginLayout extends VerticalLayout implements HasUrlParameter<String> {

  private static final Logger log = LoggerFactory.getLogger(LoginLayout.class);
  private VerticalLayout contentLayout;

  public VerticalLayout notificationLayout;
  private H2 title;

  private ConfigurableLoginForm loginForm;

  private Div registrationSection;

  private final transient LoginHandlerInterface viewHandler;

  private final static String OrcId_LOGO_PATH = "login/orcid_logo.svg";

  public LoginLayout(@Autowired LoginHandlerInterface loginHandlerInterface,
      @Value("${server.servlet.context-path}") String contextPath) {
    initLayout(contextPath);
    styleLayout();
    viewHandler = loginHandlerInterface;
    registerToHandler(viewHandler);
  }

  private void initLayout(final String contextPath) {
    contentLayout = new VerticalLayout();
    createNotificationLayout();
    createLoginForm();
    registrationSection = initRegistrationSection(contextPath);
    title = new H2("Log in");
    contentLayout.add(title, notificationLayout, loginForm, registrationSection);
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

  private Div initRegistrationSection(String contextPath) {
    RouterLink routerLink = new RouterLink("Register", UserRegistrationMain.class);
    Span registrationLink = new Span(new Text("Don't have an account? "), routerLink);
    registrationLink.addClassName("registration-link");
    Span spacer = new Span("OR");
    spacer.addClassName("spacer");
    LoginCard orcidCard = new LoginCard(getOrcIdSource(), "Login with ORCID",
        contextPath + "/oauth2/authorization/orcid");
    Div registrationSection = new Div(registrationLink, spacer, orcidCard);
    registrationSection.addClassName("registration-section");
    return registrationSection;
  }

  private AbstractStreamResource getOrcIdSource() {
    return new StreamResource("orcid_logo.svg",
        () -> getClass().getClassLoader().getResourceAsStream(OrcId_LOGO_PATH));
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
      addClickListener(event -> UI.getCurrent().getPage().open(url, "_blank"));
    }
  }

}
