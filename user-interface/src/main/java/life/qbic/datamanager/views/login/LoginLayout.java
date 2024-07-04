package life.qbic.datamanager.views.login;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

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
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.register.UserRegistrationMain;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.UserNotFoundException;
import life.qbic.logging.api.Logger;
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
@UIScope
public class LoginLayout extends VerticalLayout implements HasUrlParameter<String> {

  private static final Logger log = logger(LoginLayout.class);
  private final String emailConfirmationParameter;
  public VerticalLayout notificationLayout;
  private VerticalLayout contentLayout;
  private H2 title;
  private ConfigurableLoginForm loginForm;
  private Div registrationSection;
  private final transient IdentityService identityService;
  private static final String ORCID_LOGO_PATH = "login/orcid_logo.svg";

  public LoginLayout(@Autowired LoginHandler loginHandler,
      @Autowired IdentityService identityService,
      @Value("${server.servlet.context-path}") String contextPath) {
    requireNonNull(loginHandler, "loginHandler must not be null");
    this.identityService = requireNonNull(identityService,
        "identityService must not be null");
    this.emailConfirmationParameter = requireNonNull(
        loginHandler.emailConfirmationParameter(), "email confirmationParameter must not be null");
    initLayout(contextPath);
    styleLayout();
    initFields();
    addListener();
  }

  private void initLayout(final String contextPath) {
    contentLayout = new VerticalLayout();
    createNotificationLayout();
    createLoginForm();
    registrationSection = initRegistrationSection(contextPath);
    registrationSection.addClassName("registration-section");
    title = new H2("Log in");
    contentLayout.add(title, notificationLayout, loginForm, registrationSection);
    add(contentLayout);
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
    return new Div(registrationLink, spacer, orcidCard);
  }

  private AbstractStreamResource getOrcIdSource() {
    return new StreamResource("orcid_logo.svg",
        () -> getClass().getClassLoader().getResourceAsStream(ORCID_LOGO_PATH));
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
    handle(event);
  }

  private void initFields() {
    clearNotifications();
  }

  private void showInvalidCredentialsError() {
    showError("Incorrect username or password", "Please try again.");
  }

  private void showEmailConfirmationInformation() {
    showInformation("Email address confirmed", "You can now login with your credentials.");
  }

  private void showEmailConfirmationReminder() {
    showInformation("Registration mail sent",
        "Please check your mail inbox to confirm your registration");
  }

  public void clearNotifications() {
    notificationLayout.removeAll();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    notificationLayout.add(errorMessage);
  }

  public void showInformation(String title, String description) {
    clearNotifications();
    InformationMessage informationMessage = new InformationMessage(title, description);
    notificationLayout.add(informationMessage);
  }

  private void addListener() {
    addLoginListener(it ->
        onLoginSucceeded());
    addForgotPasswordListener(
        it -> it.getSource().getUI().ifPresent(ui -> ui.navigate(AppRoutes.RESET_PASSWORD)));
  }

  private void onLoginSucceeded() {
    clearNotifications();
    getUI().ifPresentOrElse(
        ui -> ui.navigate(Projects.PROJECTS),
        () -> log.error("No UI found!"));
  }

  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> queryParams = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    if (queryParams.containsKey("error")) {
      showInvalidCredentialsError();
    }
    if (queryParams.containsKey(emailConfirmationParameter)) {
      String userId = queryParams.get(emailConfirmationParameter).iterator().next();
      try {
        identityService.confirmUserEmail(userId);
        onEmailConfirmationSuccess();
      } catch (UserNotFoundException e) {
        log.error("User %s not found!".formatted(userId), e);
        onEmailConfirmationFailure(
            "Unknown user for request. If the issue persists, please contact our helpdesk.");
      }

    }
    if (queryParams.containsKey("userRegistered")) {
      showEmailConfirmationReminder();
    }
  }

  public void onEmailConfirmationSuccess() {
    showEmailConfirmationInformation();
  }

  public void onEmailConfirmationFailure(String reason) {
    showError("Email confirmation failed", reason);
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
