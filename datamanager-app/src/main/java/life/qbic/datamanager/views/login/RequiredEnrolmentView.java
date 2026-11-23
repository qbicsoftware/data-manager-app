package life.qbic.datamanager.views.login;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Key;
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
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.time.Instant;
import life.qbic.datamanager.security.mfa.MfaPendingLogin;
import life.qbic.datamanager.security.mfa.MfaSessionCompletion;
import life.qbic.datamanager.views.account.RecoveryCodesDialog;
import life.qbic.datamanager.views.account.TotpQrCode;
import life.qbic.datamanager.views.general.CopyToClipboardButton;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.identity.application.mfa.EnrolmentExpiredException;
import life.qbic.identity.application.mfa.InvalidTotpCodeException;
import life.qbic.identity.application.mfa.PendingTotpEnrolment;
import life.qbic.identity.application.mfa.RecoveryCodeService;
import life.qbic.identity.application.mfa.RecoveryCodeSet;
import life.qbic.identity.application.mfa.TotpEnrolmentService;
import life.qbic.identity.domain.model.mfa.Totp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * <b>Enrolment as a step of signing in, for an account that is not owed a deadline</b>
 *
 * <p>This is the registration path. An account created after the required posture came into force is
 * not granted a grace window ({@code MfaGateDecision.EnrolmentDue}), so its first login does not
 * become a session — it becomes this page. The password was verified, the authentication was
 * withdrawn, and a {@link MfaPendingLogin} is the only thing that came of it.</p>
 *
 * <h3>Why enrolment lives here and not in the registration form</h3>
 * <p>Signing up and proving you can produce a code are two different promises. The registration form
 * collects what an account needs; this step proves the factor exists <em>before</em> the account is
 * usable, and it reuses the same service the settings page uses. It also means an abandoned signup
 * leaves a real account behind rather than a half-written one — a state support can look at.</p>
 *
 * <h3>The order of the last two steps is deliberate</h3>
 * <p>The recovery codes are shown <em>before</em> the session exists, and the page does not continue
 * until the user acknowledges them. That is the only moment at which they are guaranteed to be looking
 * at the screen, and those codes are the only self-service way back in if the phone is lost.</p>
 *
 * <h3>What this page refuses to promise</h3>
 * <p>An account whose owner cannot install an authenticator app has no path through this page. The copy
 * says so and names the contact address rather than offering a way out that does not exist — the
 * alternative factor types do not exist yet, and a "skip" would be the end of the requirement.</p>
 *
 * @since 1.0.0
 */
@com.vaadin.flow.component.dependency.CssImport("./styles/views/login/login-view.css")
@PageTitle("Set up two-factor authentication")
@Route(value = "login/enrol", layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
public class RequiredEnrolmentView extends VerticalLayout implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Where the login path sends an account that must enrol; used by the filter and the success handler. */
  public static final String PATH = "/login/enrol";

  /**
   * The label the factor carries. One factor per account is created here, so the account's own email
   * would only repeat what the entry already shows; "Authenticator app" reads better in a list of
   * several devices later.
   */
  static final String DISPLAY_NAME = "Authenticator app";

  private final transient TotpEnrolmentService enrolmentService;
  private final transient RecoveryCodeService recoveryCodeService;
  private final transient MfaSessionCompletion sessionCompletion;
  private final transient UserDetailsService userDetailsService;
  private final String contactEmail;

  private final Div form = new Div();
  private final TextField code = new TextField("Code from the app");
  private final Span error = new Span();

  private MfaPendingLogin pending;
  private PendingTotpEnrolment enrolment;

  public RequiredEnrolmentView(TotpEnrolmentService enrolmentService,
      RecoveryCodeService recoveryCodeService,
      MfaSessionCompletion sessionCompletion,
      UserDetailsService userDetailsService,
      @Value("${qbic.communication.contact.email}") String contactEmail) {
    this.enrolmentService = requireNonNull(enrolmentService, "enrolmentService must not be null");
    this.recoveryCodeService = requireNonNull(recoveryCodeService,
        "recoveryCodeService must not be null");
    this.sessionCompletion = requireNonNull(sessionCompletion,
        "sessionCompletion must not be null");
    this.userDetailsService = requireNonNull(userDetailsService,
        "userDetailsService must not be null");
    this.contactEmail = requireNonNull(contactEmail, "contactEmail must not be null");

    // Top-aligned, not centred: this card is taller than the login card, and centring a too-tall
    // element pushes it over the footer instead of letting the page scroll.
    setAlignItems(FlexComponent.Alignment.CENTER);
    getStyle().set("padding-top", "var(--lumo-space-xl)");
    getStyle().set("padding-bottom", "var(--lumo-space-xl)");
    form.addClassName("second-factor-card");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    HttpSession session = currentSession();
    var candidate = MfaPendingLogin.usableFrom(session, Instant.now());
    if (candidate.isEmpty()) {
      // Not an entry point on its own: without a verified password in this session there is nobody to
      // enrol, and rendering the form would invite an unauthenticated write.
      MfaPendingLogin.clear(session);
      event.rerouteTo(LoginLayout.class);
      return;
    }
    pending = candidate.get();
    render();
  }

  private void render() {
    Icon shield = VaadinIcon.SHIELD.create();
    shield.addClassName("second-factor-icon");
    H2 heading = new H2("Set up two-factor authentication");
    Div headingRow = new Div(shield, heading);
    headingRow.addClassName("second-factor-heading");

    Paragraph intro = new Paragraph(
        "This installation requires a second factor at sign-in. Add the Data Manager to your "
            + "authenticator app, then type the code it shows to finish signing in.");

    enrolment = enrolmentService.begin(pending.userId(), DISPLAY_NAME, pending.email(),
        Instant.now());

    Div qrBlock = new Div(new TotpQrCode(enrolment.provisioningUri()));
    qrBlock.addClassName("mfa-qr-block");

    TextField setupKey = new TextField("Setup key (if you can't scan)");
    setupKey.setValue(setupKeyFrom(enrolment.provisioningUri()));
    setupKey.setReadOnly(true);
    setupKey.setWidthFull();
    CopyToClipboardButton copy = new CopyToClipboardButton("Copy");
    copy.addClickListener(event -> copy.copy(setupKey.getValue()));
    Div setupKeyRow = new Div(setupKey, copy);
    setupKeyRow.addClassName("mfa-setup-key-row");

    code.setWidthFull();
    // Exactly six digits: a longer field lets a typo hide behind a valid prefix, and the verifier only
    // ever accepts six (Totp.DIGITS).
    code.setMaxLength(Totp.DIGITS);
    code.setAllowedCharPattern("\\d");
    code.setAutofocus(true);
    code.setPlaceholder("123456");

    Button verify = new Button("Verify and continue");
    verify.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    verify.addClickShortcut(Key.ENTER);
    verify.addClickListener(event -> verifyCode());
    verify.setId("verify-enrolment");

    error.getStyle().set("color", "var(--lumo-error-text-color)");
    error.setVisible(false);

    Div codeField = new Div(code, verify);
    codeField.addClassName("second-factor-field");

    Anchor support = new Anchor("mailto:" + contactEmail, contactEmail);
    Paragraph noApp = new Paragraph(
        "Cannot install an authenticator app? Two-factor authentication is required on this "
            + "installation and there is no other way in yet. Contact your QBiC support: ");
    noApp.add(support);
    noApp.addClassName(Margin.Top.LARGE);

    form.add(headingRow, intro, qrBlock, setupKeyRow, codeField, error, noApp);
    add(form);
  }

  private void verifyCode() {
    if (pending == null || enrolment == null) {
      return;
    }
    String presented = code.getValue() == null ? "" : code.getValue().strip();
    if (presented.isEmpty()) {
      showError("Enter the six-digit code shown by the authenticator app.");
      return;
    }
    try {
      enrolmentService.confirm(enrolment, presented, Instant.now());
    } catch (InvalidTotpCodeException e) {
      // The one case worth advising on: a phone whose clock has drifted is the usual cause.
      showError("That code is not valid. Check that the phone's clock is set automatically, then try "
          + "the current code.");
      return;
    } catch (EnrolmentExpiredException e) {
      showError("This setup page has expired. Sign in again to start with a fresh code.");
      return;
    }

    RecoveryCodeSet codes = recoveryCodeService.generate(pending.userId(), Instant.now());
    RecoveryCodesDialog dialog = new RecoveryCodesDialog(codes, pending.email(),
        RecoveryCodesDialog.Reason.FIRST_FACTOR);
    // The session is created only after the acknowledgement: the codes are shown once, and a login
    // that completes before the user has looked at them cannot be undone.
    dialog.addCodesSavedListener(event -> sessionCompletion.complete(pending));
    dialog.open();
  }

  private void showError(String message) {
    error.setText(message);
    error.setVisible(true);
    code.setInvalid(true);
  }

  /**
   * Reads the shared key out of the {@code otpauth://} URI.
   *
   * <p>The URI is the domain's interface for "what the authenticator needs"; the manual-entry field is
   * just a second rendering of it. Same helper as the settings dialog, deliberately duplicated in
   * behaviour if not in code — both must read the same parameter.</p>
   */
  static String setupKeyFrom(String provisioningUri) {
    String marker = "secret=";
    int start = provisioningUri.indexOf(marker);
    if (start < 0) {
      throw new IllegalArgumentException("the provisioning URI does not contain a secret parameter");
    }
    int valueStart = start + marker.length();
    int end = provisioningUri.indexOf('&', valueStart);
    return end < 0
        ? provisioningUri.substring(valueStart)
        : provisioningUri.substring(valueStart, end);
  }

  private static HttpSession currentSession() {
    VaadinRequest request = VaadinService.getCurrentRequest();
    return request instanceof VaadinServletRequest servletRequest
        ? servletRequest.getSession(false)
        : null;
  }
}
