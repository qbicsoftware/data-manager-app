package life.qbic.datamanager.views.login;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import life.qbic.identity.domain.model.mfa.RecoveryCodes;
import life.qbic.identity.domain.model.mfa.Totp;
import life.qbic.datamanager.security.mfa.MfaPendingLogin;
import life.qbic.datamanager.security.mfa.MfaSessionCompletion;
import life.qbic.identity.application.mfa.MfaReenrolmentService;
import life.qbic.datamanager.security.mfa.MfaSessionState;
import life.qbic.datamanager.views.account.MfaEnrolmentPrompt;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.identity.application.mfa.RecoveryCodeService;
import life.qbic.identity.application.throttle.AuthAttemptThrottle;
import life.qbic.identity.application.mfa.SecondFactorVerificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

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
@CssImport("./styles/views/login/login-view.css")
@JsModule("./javascript/recoverycodeformat.js")
@PageTitle("Second factor")
@Route(value = "login/second-factor", layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
public class SecondFactorView extends VerticalLayout implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  private final transient SecondFactorVerificationService verificationService;
  private final transient RecoveryCodeService recoveryCodeService;
  private final transient MfaSessionCompletion sessionCompletion;
  private final transient MfaReenrolmentService reenrolmentService;
  /**
   * Consulted only to render the wait, never to decide the refusal.
   *
   * <p>The decision lives in the two services, so a future page cannot forget to ask; this reference
   * exists so the sentence says "15 minutes" instead of a hard-coded guess. {@code null} in tests that
   * do not exercise the throttle.</p>
   */
  private final transient AuthAttemptThrottle throttle;

  private final TextField code = new TextField("Six-digit code");
  private final TextField recoveryCode = new TextField("Recovery code");
  private final Span error = new Span();
  private final Button verify = new Button("Verify");
  private final Button verifyRecovery = new Button("Sign in with recovery code");
  private final Paragraph instructions = new Paragraph();
  private final Button useRecovery = new Button();
  private final Div codeSection = new Div();
  private final Div recoverySection = new Div();
  private final Div form = new Div();

  private MfaPendingLogin pending;

  public SecondFactorView(SecondFactorVerificationService verificationService,
      RecoveryCodeService recoveryCodeService, MfaSessionCompletion sessionCompletion,
      MfaReenrolmentService reenrolmentService,
      @Value("${qbic.communication.contact.email}") String contactEmail,
      AuthAttemptThrottle throttle) {
    this.verificationService = requireNonNull(verificationService,
        "verificationService must not be null");
    this.recoveryCodeService = requireNonNull(recoveryCodeService,
        "recoveryCodeService must not be null");
    this.sessionCompletion = requireNonNull(sessionCompletion,
        "sessionCompletion must not be null");
    this.reenrolmentService = requireNonNull(reenrolmentService,
        "reenrolmentService must not be null");
    this.throttle = throttle;

    // Top-aligned, not centred: the card is taller than the login card, and centring a too-tall
    // element pushes it over the footer instead of letting the page scroll.
    setAlignItems(FlexComponent.Alignment.CENTER);
    getStyle().set("padding-top", "var(--lumo-space-xl)");
    getStyle().set("padding-bottom", "var(--lumo-space-xl)");

    form.addClassName("second-factor-card");

    code.setWidthFull();
    // Exactly six digits: a longer field lets a typo hide behind a valid prefix, and the verifier
    // only ever accepts six (Totp.DIGITS).
    code.setMaxLength(Totp.DIGITS);
    code.setAllowedCharPattern("\\d");
    code.setAutofocus(true);
    code.setPlaceholder("123456");

    error.getStyle().set("color", "var(--lumo-error-text-color)");
    error.setVisible(false);

    verify.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    verify.addClickShortcut(Key.ENTER);
    verify.addClickListener(event -> verifyCode());

    recoveryCode.setWidthFull();
    // 12 characters plus the two grouping hyphens; the pattern blocks characters outside the
    // alphabet, so 0/1/8/9 cannot be typed into a code that never contains them.
    recoveryCode.setMaxLength(RecoveryCodes.CODE_LENGTH + RecoveryCodes.CODE_LENGTH / 4 - 1);
    recoveryCode.setAllowedCharPattern("[A-Za-z2-7\\-]");
    recoveryCode.setPlaceholder("XXXX-XXXX-XXXX");
    // Grouping *while typing* happens in the browser (frontend/javascript/recoverycodeformat.js,
    // imported above). Two Java attempts failed before it:
    //   1. a server-side value-change listener: Vaadin's default value-change mode is LAZY, so it ran
    //      only when the field lost focus;
    //   2. an inline executeJs listener installed on attach with a retry loop: it never attached, and
    //      a script that does nothing logs nothing, so there was no signal at all.
    // The script is delegated on the document and marks the fields it acts on with this attribute, so
    // it needs no per-element install and cannot lose an attach race.
    recoveryCode.getElement().setAttribute("data-recovery-code-input", "");

    // Fallback for a browser where the script did not load: one formatting pass when the field loses
    // focus (which is when a LAZY value change reaches the server). Idempotent, and the encoder
    // normalises again before hashing, so the stored value never depends on what the browser showed.
    recoveryCode.addValueChangeListener(event -> {
      if (!event.isFromClient() || event.getValue() == null) {
        return;
      }
      String formatted = formatRecoveryCodeInput(event.getValue());
      if (!formatted.equals(event.getValue())) {
        recoveryCode.setValue(formatted);
      }
    });

    verifyRecovery.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    verifyRecovery.addClickListener(event -> verifyRecoveryCode());

    // Enter must submit whichever mode is active: the recovery field is the one a panicking user
    // fills in, and it must not need a mouse.
    verifyRecovery.addClickShortcut(Key.ENTER);

    codeSection.addClassName("second-factor-field");
    recoverySection.addClassName("second-factor-field");
    codeSection.add(code, verify);
    recoverySection.add(recoveryCode, verifyRecovery);
    recoverySection.setVisible(false);

    // The break-glass path (AUTH-R-03, M4-3): a lost or replaced phone must not require an
    // administrator. The codes are single-use and were shown once, at generation.
    useRecovery.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    useRecovery.addClickListener(event -> showRecoveryMode(!recoverySection.isVisible()));

    // Someone with neither factor has no self-service way back in. What this paragraph must NOT do is
    // advertise "email support and they will reset it": the account was created by self-service
    // registration with nothing but email verification, so that would make control of an inbox the
    // identity proof for a factor reset — the cheapest way past the requirement, and a social
    // engineering target aimed at exactly the account an attacker wants.
    //
    // So it names a contact, states plainly that email cannot grant a reset, and points at the
    // identity check that has to happen instead. The address comes from configuration.
    Paragraph noFactorLeft = new Paragraph(
        "Lost your authenticator app and your recovery codes? A reset needs identity verification "
            + "and cannot be granted by email. Ask QBiC support who can verify you: ");
    noFactorLeft.add(new Anchor("mailto:" + contactEmail, contactEmail));

    RouterLink useAnotherAccount = new RouterLink("Use a different account", LoginLayout.class);
    Div footerLink = new Div(useAnotherAccount);
    footerLink.addClassName("second-factor-footer");

    // The heading carries the shield so the step reads as a security checkpoint at a glance, rather
    // than as one more field on the login page.
    Icon shield = VaadinIcon.SHIELD.create();
    shield.addClassName("second-factor-icon");
    Div heading = new Div(shield, new H2("Second factor"));
    heading.addClassName("second-factor-heading");

    Div links = new Div(useRecovery);
    links.addClassName("second-factor-links");

    form.add(heading, instructions, codeSection, recoverySection, error, links, noFactorLeft,
        footerLink);
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
    if (result.outcome() == SecondFactorVerificationService.Outcome.THROTTLED) {
      showThrottledNotice();
      return;
    }
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

  /**
   * Brings a partially typed recovery code into {@code XXXX-XXXX-XXXX} shape.
   *
   * <p>Tolerant by design: it runs on every keystroke, so it must accept a value that is still too
   * short or temporarily carries a half-typed hyphen. Anything that is not alphabet is dropped and the
   * result is truncated to the code length — the same normalisation {@code RecoveryCodeEncoder} applies
   * before hashing, just without the strictness that belongs to submission rather than to typing.</p>
   *
   * @param raw whatever is currently in the field
   * @return the grouped, upper-case prefix of a recovery code
   */
  static String formatRecoveryCodeInput(String raw) {
    if (raw == null) {
      return "";
    }
    String characters = raw.replaceAll("[^A-Za-z2-7]", "").toUpperCase(Locale.ROOT);
    if (characters.length() > RecoveryCodes.CODE_LENGTH) {
      characters = characters.substring(0, RecoveryCodes.CODE_LENGTH);
    }
    StringBuilder formatted = new StringBuilder(characters.length() + 2);
    for (int index = 0; index < characters.length(); index++) {
      if (index > 0 && index % 4 == 0) {
        formatted.append('-');
      }
      formatted.append(characters.charAt(index));
    }
    return formatted.toString();
  }

  private void verifyRecoveryCode() {
    if (pending == null) {
      return;
    }
    var redemption = recoveryCodeService.redeemFor(pending.userId(), recoveryCode.getValue(),
        Instant.now());
    if (redemption.throttled()) {
      showThrottledNotice();
      return;
    }
    if (redemption.redeemed()) {
      // The set may now be empty. Nothing else in the flow can tell the user that: the settings page
      // counts what is left, and someone signing in with a recovery code is mid-incident rather than
      // browsing settings (MFA-41b).
      warnAboutRemainingRecoveryCodes();
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
   * Tells the user they have run out of attempts, and for how long.
   *
   * <p>The one refusal this page describes accurately. Everything else it says about a failed code is
   * deliberately uniform (AUTH-NFR-03), because distinguishing "wrong code" from "no such factor"
   * helps an attacker. A rate limit is different in both directions: reaching it required the correct
   * password, so the caller already knows the account exists, and hiding it produces the worst
   * possible experience — a user typing codes that cannot possibly work, told each time that the code
   * was wrong. Silence here is not security, it is a dead end with no explanation.</p>
   *
   * <p>The controls are disabled rather than merely disheartened: leaving them live invites another
   * attempt on a page that will refuse it, and burns nothing but the user's patience.</p>
   */
  private void showThrottledNotice() {
    error.setText(throttledMessage(blockDuration()));
    error.setVisible(true);
    code.setEnabled(false);
    verify.setEnabled(false);
    recoveryCode.setEnabled(false);
    verifyRecovery.setEnabled(false);
  }

  /**
   * <b>"Too many attempts."</b>
   *
   * <p>States what happened, how long it lasts, and that nothing has been lost — the last part is what
   * stops a worried user from assuming the account is now locked out for good and calling support.</p>
   *
   * @param blockDuration how long the throttle lasts
   * @return the sentence
   */
  static String throttledMessage(Duration blockDuration) {
    return "Too many attempts. For your account's security, further attempts are blocked for "
        + describeDuration(blockDuration) + ". Your password and your authenticator app still work — "
        + "try again when the wait is over.";
  }

  private Duration blockDuration() {
    return throttle == null ? Duration.ofMinutes(15)
        : throttle.blockDuration(AuthAttemptThrottle.Scope.SECOND_FACTOR);
  }

  /**
   * Renders a duration as the smallest whole unit a person would say out loud.
   *
   * <p>"PT15M" is not a sentence, and "900 seconds" makes a user do arithmetic. Minutes for an hour or
   * less, hours beyond that; the configured values are quarter-hours, so nothing finer is needed.</p>
   *
   * @param duration the block duration
   * @return e.g. {@code "15 minutes"} or {@code "1 hour"}
   */
  static String describeDuration(Duration duration) {
    long minutes = Math.max(1, duration.toMinutes());
    if (minutes < 60) {
      return minutes == 1 ? "1 minute" : minutes + " minutes";
    }
    long hours = minutes / 60;
    long remainder = minutes % 60;
    String hoursPart = hours == 1 ? "1 hour" : hours + " hours";
    return remainder == 0 ? hoursPart : hoursPart + " " + remainder + " minutes";
  }

  /**
   * Says how many recovery codes are left, and says it loudly when there are none.
   *
   * <p>Recorded as the last thing before the session is established, so it survives the redirect and
   * is read on the page the user lands on. An empty set is the state that turns a lost phone into a
   * helpdesk ticket, and the moment just after redeeming the last code is the only moment the user is
   * certainly paying attention to it.</p>
   */
  private void warnAboutRemainingRecoveryCodes() {
    // In the session, not in `error.setText(...)`: the very next statement establishes the session
    // and redirects, so anything written into this page's components is discarded one instruction
    // later. MfaEnrolmentPrompt is mounted in the application shell, reads this on attach and shows
    // it at the top of the page the user lands on.
    HttpSession session = currentSession();
    if (session == null) {
      return;
    }
    session.setAttribute(MfaEnrolmentPrompt.RECOVERY_CODES_LEFT_ATTRIBUTE,
        (int) recoveryCodeService.remaining(pending.userId()));
  }

  /**
   * Establishes the authentication the password alone did not get.
   *
   * <p>Delegated to {@link MfaSessionCompletion}, which is shared with the mandatory-enrolment page: the
   * rules (reload the principal, mark the session, clear the pending login, honour the saved request and
   * the context path) are the same for both, and having them in two places is how one of them drifts.</p>
   */
  private void establishSession() {
    sessionCompletion.complete(pending);
  }

  /**
   * The state after the enrolment window ran out: no code to enter, and a way forward that exists.
   *
   * <p>The previous copy said "ask an administrator to re-enrol you". There was no such action — the
   * grace window cannot be reopened by the application, and no administrator has a button that grants
   * one — so it sent users to a person who could only escalate to a database update. The button below
   * is the recovery path that exists: a one-time link to this account's mailbox, which opens the
   * enrolment page without granting a factor (finishing it still needs the device).</p>
   *
   * <p>An account with <b>an active factor</b> gets no link from this button; the service refuses it
   * silently, because a mailbox must not be the way past a factor that already exists. That user needs
   * identity verification by the helpdesk, and the copy below says so.</p>
   */
  private void showGraceExpired() {
    codeSection.setVisible(false);
    recoverySection.setVisible(false);
    error.setVisible(false);

    Paragraph expired = new Paragraph(
        "Your enrolment window for two-factor authentication has passed, and signing in needs a "
            + "factor. You can set one up now: we send a one-time link to your account's e-mail "
            + "address.");
    Button sendLink = new Button("E-mail me a setup link");
    sendLink.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sendLink.addClickListener(event -> {
      if (pending == null) {
        UI.getCurrent().getPage().setLocation(loginPath());
        return;
      }
      // The same call for every account state; the service answers uniformly (AUTH-NFR-03), so this
      // page cannot be used to learn whether an address has an account.
      reenrolmentService.request(pending.email(), Instant.now());
      sendLink.setEnabled(false);
      Span sent = new Span(
          "If that address belongs to an account without a factor, the link is on its way. It works "
              + "once and expires after 15 minutes.");
      sent.addClassName("second-factor-notice");
      form.add(sent);
    });

    Paragraph noMailbox = new Paragraph(
        "No access to that mailbox, or an authenticator app you no longer have? A reset then needs "
            + "identity verification and cannot be granted by e-mail. Ask QBiC support who can verify "
            + "you in person.");
    noMailbox.addClassName("second-factor-support");

    form.add(expired, sendLink, noMailbox);
  }

  /**
   * @return the login path including the context path, e.g. {@code /dev/login}
   */
  private static String loginPath() {
    var request = VaadinService.getCurrentRequest();
    String contextPath = request == null ? "" : request.getContextPath();
    return (contextPath == null ? "" : contextPath) + "/login";
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
