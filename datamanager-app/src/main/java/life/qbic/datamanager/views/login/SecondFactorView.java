package life.qbic.datamanager.views.login;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.Serial;
import java.time.Instant;
import life.qbic.datamanager.security.mfa.MfaPendingLogin;
import life.qbic.datamanager.security.mfa.MfaSessionState;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.identity.application.mfa.RecoveryCodeService;
import life.qbic.identity.application.mfa.SecondFactorVerificationService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;

/**
 * <b>The second-factor step (M4-1)</b>
 *
 * <p>Shown only to a session that is mid-login: the password was accepted, and {@link
 * life.qbic.datamanager.security.mfa.MfaLoginSuccessHandler} withheld the authentication. Without a
 * pending login this page is not a page — it reroutes to {@code /login}, so it cannot be used as an
 * entry point.</p>
 *
 * <p><b>Failure says nothing specific.</b> A wrong code, a code outside the tolerated clock skew and a
 * factor that cannot be decrypted all produce the same sentence, because the differences are only
 * useful to an attacker (AUTH-NFR-03). The one case that speaks plainly is an expired enrolment
 * window: that user authenticated with the correct password and needs to know to call support.</p>
 *
 * <h3>Why the session is not rotated again here</h3>
 * <p>Spring rotates the session id inside the password filter, before this page is reachable, so the
 * id that carries the pending login is already a fresh one that no attacker who fixed a session before
 * login could know. Rotating again inside a Vaadin UI event would mean changing the id underneath a
 * live Vaadin session, which is a bigger risk than the fixation window it closes. The property still
 * holds: the authentication is only ever created in a session that was rotated at the password step.</p>
 *
 * @since 1.0.0
 */
@PageTitle("Second factor")
@Route(value = "login/second-factor", layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
public class SecondFactorView extends VerticalLayout implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  private final transient SecondFactorVerificationService verificationService;
  private final transient RecoveryCodeService recoveryCodeService;
  private final transient UserDetailsService userDetailsService;
  private final transient VaadinDefaultRequestCache requestCache;

  private final TextField code = new TextField("Six-digit code");
  private final TextField recoveryCode = new TextField("Recovery code");
  private final Span error = new Span();
  private final Button verify = new Button("Verify");
  private final Button verifyRecovery = new Button("Sign in with recovery code");
  private final Paragraph instructions = new Paragraph();
  private final Button useRecovery = new Button();
  private final Div codeSection = new Div();
  private final Div recoverySection = new Div();
  private final VerticalLayout form = new VerticalLayout();

  private MfaPendingLogin pending;

  public SecondFactorView(SecondFactorVerificationService verificationService,
      RecoveryCodeService recoveryCodeService,
      UserDetailsService userDetailsService, VaadinDefaultRequestCache requestCache) {
    this.verificationService = requireNonNull(verificationService,
        "verificationService must not be null");
    this.recoveryCodeService = requireNonNull(recoveryCodeService,
        "recoveryCodeService must not be null");
    this.userDetailsService = requireNonNull(userDetailsService,
        "userDetailsService must not be null");
    this.requestCache = requireNonNull(requestCache, "requestCache must not be null");

    setAlignItems(FlexComponent.Alignment.CENTER);
    setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

    form.setPadding(false);
    form.addClassNames("bg-base", "border", "border-contrast-10", "rounded-m", "box-border",
        "flex", "flex-col", "w-full", "text-s", "shadow-l", "min-width-300px", "max-width-15vw",
        "pb-l", "pr-l", "pl-l");

    code.setWidthFull();
    code.setMaxLength(8);
    code.setAutofocus(true);
    code.setPlaceholder("123456");

    error.getStyle().set("color", "var(--lumo-error-text-color)");
    error.setVisible(false);

    verify.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    verify.addClickShortcut(Key.ENTER);
    verify.addClickListener(event -> verifyCode());

    recoveryCode.setWidthFull();
    recoveryCode.setPlaceholder("XXXX-XXXX-XXXX");
    verifyRecovery.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    verifyRecovery.addClickListener(event -> verifyRecoveryCode());

    // Enter must submit whichever mode is active: the recovery field is the one a panicking user
    // fills in, and it must not need a mouse.
    verifyRecovery.addClickShortcut(Key.ENTER);

    codeSection.add(code, verify);
    recoverySection.add(recoveryCode, verifyRecovery);
    recoverySection.setVisible(false);

    // The break-glass path (AUTH-R-03, M4-3): a lost or replaced phone must not require an
    // administrator. The codes are single-use and were shown once, at generation.
    useRecovery.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    useRecovery.addClickListener(event -> showRecoveryMode(!recoverySection.isVisible()));

    // Someone with neither factor has no self-service way back in. Saying who to ask is the
    // difference between "locked out" and "one email away from a re-enrolment".
    Paragraph noFactorLeft = new Paragraph(
        "Lost your authenticator app and your recovery codes? Ask your data steward to re-enrol "
            + "you — an administrator can reset the second factor for your account.");

    RouterLink useAnotherAccount = new RouterLink("Use a different account", LoginLayout.class);

    form.add(new H2("Second factor"), instructions, codeSection, recoverySection, error,
        useRecovery, noFactorLeft, useAnotherAccount);
    form.getStyle().set("max-width", "24rem");
    showRecoveryMode(false);
    add(form);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    var session = currentSession();
    var candidate = MfaPendingLogin.usableFrom(session, Instant.now());
    if (candidate.isEmpty()) {
      // No (or an expired) pending login: this route is not an entry point.
      MfaPendingLogin.clear(session);
      event.rerouteTo(LoginLayout.class);
      return;
    }
    pending = candidate.get();
    if (event.getLocation().getQueryParameters().getParameters().containsKey("graceExpired")) {
      showGraceExpired();
    }
  }

  private void verifyCode() {
    if (pending == null) {
      return;
    }
    var result = verificationService.verify(pending.userId(), code.getValue(), Instant.now());
    if (!result.verified()) {
      // Same message for every refusal reason (AUTH-NFR-03). The attempt stays pending: the user may
      // try the next code without re-entering the password.
      error.setText("That code was not accepted. Try the current code from your app.");
      error.setVisible(true);
      code.setValue("");
      return;
    }
    establishSession();
  }

  /**
   * Switches between the authenticator code and a recovery code.
   *
   * <p>The control, the instruction and the visible field change together: a toggle that keeps saying
   * "use a recovery code instead" while the recovery field is already on screen is a control that lies
   * about its current state. Focus moves to the field that just appeared, so keyboard users are not
   * left behind.</p>
   *
   * @param recovery {@code true} for the recovery-code mode
   */
  private void showRecoveryMode(boolean recovery) {
    recoverySection.setVisible(recovery);
    codeSection.setVisible(!recovery);
    if (recovery) {
      instructions.setText("Enter one of the recovery codes you saved when you set up "
          + "two-factor authentication. Each code works once.");
      useRecovery.setText("Use the authenticator app instead");
      error.setVisible(false);
      recoveryCode.focus();
    } else {
      instructions.setText("Enter the six-digit code from your authenticator app.");
      useRecovery.setText("Use a recovery code instead");
      error.setVisible(false);
      code.focus();
    }
  }

  private void verifyRecoveryCode() {
    if (pending == null) {
      return;
    }
    if (recoveryCodeService.redeemFor(pending.userId(), recoveryCode.getValue(), Instant.now())) {
      establishSession();
      return;
    }
    // Same sentence as a wrong TOTP code, on purpose (AUTH-NFR-03): the difference between "unknown",
    // "used" and "belongs to another account" is only useful to an attacker.
    error.setText("That recovery code was not accepted. Each code works once.");
    error.setVisible(true);
    recoveryCode.setValue("");
  }

  /**
   * Establishes the authentication the password alone did not get.
   *
   * <p>The principal is reloaded through {@link UserDetailsService} rather than reconstructed, so
   * authorities are exactly what a password login would have produced — the factor step grants
   * nothing extra, it only completes the same login.</p>
   */
  private void establishSession() {
    UserDetails user = userDetailsService.loadUserByUsername(pending.email());
    var authentication = UsernamePasswordAuthenticationToken.authenticated(
        user, null, user.getAuthorities());

    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    HttpServletRequest request = currentHttpRequest();
    HttpServletResponse response = (HttpServletResponse) VaadinService.getCurrentResponse();
    new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    var session = request.getSession(false);
    // Marks this session as having passed the step, so the enforcement filter does not ask again on
    // every request. Without it the filter would re-decide here and start a challenge loop.
    MfaSessionState.store(session, pending.userId(), Instant.now());
    MfaPendingLogin.clear(session);

    // Deep links survive the challenge: Spring saved the original request when it redirected to the
    // login page, and that is where the user wanted to go (M4-4, for free). Its URL already contains
    // the context path.
    //
    // Without a saved request the fallback must be the application root, not the host root: this
    // deployment runs under a context path (e.g. /dev), and "/" would send the browser outside it —
    // a working login that lands on a 404.
    var saved = requestCache.getRequest(request, response);
    String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
    String target = saved == null ? contextPath + "/" : saved.getRedirectUrl();
    UI.getCurrent().getPage().setLocation(target);
  }

  private void showGraceExpired() {
    codeSection.setVisible(false);
    recoverySection.setVisible(false);
    error.setVisible(false);
    form.add(new Paragraph(
        "Your enrolment window for two-factor authentication has passed. "
            + "Ask an administrator to re-enrol you, then sign in again."));
  }

  private static jakarta.servlet.http.HttpSession currentSession() {
    VaadinRequest request = VaadinService.getCurrentRequest();
    return request instanceof VaadinServletRequest servletRequest
        ? servletRequest.getSession(false)
        : null;
  }

  private static HttpServletRequest currentHttpRequest() {
    return (HttpServletRequest) VaadinService.getCurrentRequest();
  }
}
