package life.qbic.datamanager.views.login;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.AbstractLogin.ForgotPasswordEvent;
import com.vaadin.flow.component.login.AbstractLogin.LoginEvent;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.datamanager.views.register.UserRegistrationMain;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
public class LoginLayout extends VerticalLayout implements HasUrlParameter<String>,
    BeforeEnterObserver {

  private static final Logger log = logger(LoginLayout.class);
  public VerticalLayout notificationLayout;
  private VerticalLayout contentLayout;
  private ConfigurableLoginForm loginForm;

  public LoginLayout(@Value("${server.servlet.context-path}") String contextPath) {
    initLayout(contextPath);
    styleLayout();
    initFields();
    addListener();
  }

  private void initLayout(final String contextPath) {
    contentLayout = new VerticalLayout();
    createNotificationLayout();
    createLoginForm();
    Div registrationSection = initRegistrationSection(contextPath);
    registrationSection.addClassName("registration-section");
    H2 title = new H2("Log in");
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

  /**
   * The only way off this page that is not a credential: create an account.
   *
   * <p>There used to be an ORCID card below it, which started an OAuth2 login round trip. ORCID is no
   * longer an identity provider — it is connected to an existing account from the settings page
   * (AUTH-R-07). Offering it here would be a second first-factor entry point, which is exactly what
   * the second-factor requirement cannot tolerate.</p>
   */
  private Div initRegistrationSection(String contextPath) {
    RouterLink routerLink = new RouterLink("Register", UserRegistrationMain.class);
    Span registrationLink = new Span(new Text("Don't have an account? "), routerLink);
    registrationLink.addClassName("registration-link");

    // Shown to EVERY visitor, unconditionally.
    //
    // This is where a returning user learns that ORCID no longer signs them in (AUTH-R-07). It must not
    // be conditional on "you had an ORCID account", because that condition is an account-existence
    // oracle (AUTH-NFR-03): anyone could probe it to learn whether an address is registered, and whether
    // it was registered through the provider. The sentence names no account and no address, and it is
    // true for everyone who reads it.
    var orcidNotice = new Span(new Text(
        "Signed in with ORCiD before? Use \"Forgot your password\" once to set a password for this "
            + "account."));
    orcidNotice.addClassName("login-orcid-notice");

    return new Div(registrationLink, orcidNotice);
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

  /**
   * Prevents authenticated users from seeing the login page again. Without this, a logged-in user
   * navigating to {@code /login} would be presented with the login form and could submit the login
   * action a second time, which can cause unwanted side effects (see issue #1304).
   * <p>
   * Anonymous sessions are backed by an {@link AnonymousAuthenticationToken} and must not count as
   * authenticated, otherwise unauthenticated users would never see the login form.
   */
  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    if (isAuthenticated()) {
      log.debug(
          "Authenticated user accessed the login page; redirecting to the project overview.");
      event.rerouteTo(ProjectOverviewMain.class);
      // rerouteTo renders the target but keeps the /login URL in the address bar (a rerouted
      // navigation is PROGRAMMATIC and does not push history state), so update the URL explicitly
      // to reflect that the user is now on the project overview.
      event.getUI().getPage().getHistory()
          .replaceState(null, new Location(ProjectRoutes.PROJECTS));
    }
  }

  private static boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }

  private void initFields() {
    clearNotifications();
  }

  /**
   * The one sentence every login failure produces.
   *
   * <p><b>Email address, not username.</b> The only login identifier is the e-mail address —
   * {@code UserDetailsServiceImpl} looks the account up by {@code findByEmail} and nothing else. The
   * old wording said "username", which sent users looking for a field that does not exist and implied
   * a second login path this application does not support (reported as MFA-94).</p>
   *
   * <p><b>Still one message for every cause</b> — wrong password, unknown address, unconfirmed
   * account, throttled attempt. Naming the cause would rebuild the enumeration oracle AUTH-NFR-03
   * forbids, and the throttle relies on this wording being indistinguishable. Correcting which field
   * is named is not a step toward being more specific.</p>
   */
  static final String INVALID_CREDENTIALS_TITLE = "Incorrect email address or password";

  /** The second line, deliberately uninformative. */
  static final String INVALID_CREDENTIALS_DESCRIPTION = "Please try again.";

  private void showInvalidCredentialsError() {
    showError(INVALID_CREDENTIALS_TITLE, INVALID_CREDENTIALS_DESCRIPTION);
  }

  public void clearNotifications() {
    notificationLayout.removeAll();
  }

  public void showError(String title, String description) {
    clearNotifications();
    ErrorMessage errorMessage = new ErrorMessage(title, description);
    notificationLayout.add(errorMessage);
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
        ui -> ui.navigate(ProjectRoutes.PROJECTS),
        () -> log.error("No UI found!"));
  }

  public void handle(BeforeEvent beforeEvent) {
    Map<String, List<String>> queryParams = beforeEvent.getLocation().getQueryParameters()
        .getParameters();
    // One failure state. There used to be a second: ?errorOauth2, rendered as "Login with ORCID
    // failed". Nothing produces it any more — ORCID is not a login path (AUTH-R-07) — and a branch
    // that can never be reached is worse than no branch: it reads like a supported entry point.
    if (queryParams.containsKey("error")) {
      showInvalidCredentialsError();
    }
    // Email confirmation deliberately does NOT happen here any more.
    //
    // It used to: /login?confirm-email={userId} called identityService.confirmUserEmail(userId)
    // inside beforeEnter, i.e. the state change fired on page retrieval. A mail scanner, link
    // prefetcher or corporate proxy that GETs a URL would confirm the account before the user ever
    // saw the page — and the credential was the user's persistent identifier, which is also visible
    // in ACL records.
    //
    // Confirmation now lives at /registration/confirm and requires a single-use token plus an
    // explicit click (AUTH-R-05). Old links are not redirected: decision B1 ships no compatibility
    // window, so they simply cannot be used, and the user requests a new one.
    //
    // There is deliberately no ?userRegistered notice either. It rendered "Registration mail sent"
    // from a query parameter that NOTHING produced: registration navigates to
    // /register/pending-email-confirmation, which says the same thing on its own page. A dead state
    // is worse than a missing one — it looks like a feature, gets maintained, and teaches the next
    // reader that /login?userRegistered=… is a supported entry point.
  }

  // There is no onEmailConfirmationSuccess/Failure here any more. Both were public methods with no
  // caller: confirmation no longer happens on /login at all (AUTH-R-05), and with it went the only
  // thing that produced the "Email address confirmed" notice. Keeping them would leave a second dead
  // state beside the ?userRegistered one removed above.
}
