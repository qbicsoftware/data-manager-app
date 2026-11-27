package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.Instant;
import java.util.List;
import life.qbic.datamanager.views.general.CopyToClipboardButton;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.settings.SettingsMainLayout;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.application.mfa.EnrolmentExpiredException;
import life.qbic.identity.application.mfa.InvalidTotpCodeException;
import life.qbic.identity.application.mfa.PendingTotpEnrolment;
import life.qbic.identity.application.mfa.RecoveryCodeService;
import life.qbic.identity.application.mfa.SecondFactorSecretUnavailableException;
import life.qbic.identity.application.mfa.SecondFactorService;
import life.qbic.identity.application.mfa.TotpEnrolmentService;
import life.qbic.identity.domain.model.mfa.SecondFactor;
import life.qbic.identity.domain.model.mfa.Totp;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Set up an authenticator app — as a page, not a modal</b>
 *
 * <p>Route: {@code /settings/two-factor/enrol}, hosted by {@link SettingsMainLayout} so the settings
 * navigation stays visible while the user is mid-flow.</p>
 *
 * <h3>Why this replaced a dialog</h3>
 * <p>The dialog carried a QR code, a setup key and two inputs, and its footer was falling below the
 * fold at ordinary window sizes — Confirm and Cancel unreachable, with the dialog body not scrolling.
 * Two rounds of re-tuning the hierarchy inside a fixed-height modal did not fix the class of problem,
 * because the content is simply taller than a dialog should be.</p>
 *
 * <p>It is also what this codebase already does: {@code NewGroupMain} and
 * {@code AdminGroupCreationMain} are routes, the password-reset flow is a route, and mandatory
 * enrolment ({@code RequiredEnrolmentView}) has always been one. The settings enrolment was the only
 * multi-step flow in a modal. A page scrolls, which retires MFA-93 for this surface instead of
 * re-tuning until it fits one viewport.</p>
 *
 * <h3>The flow this page owns</h3>
 * <ol>
 *   <li><b>Start.</b> {@link TotpEnrolmentService#begin} issues a seed and returns a
 *       {@link PendingTotpEnrolment} — encrypted seed plus the {@code otpauth://} URI. Nothing is
 *       persisted, so a user who scans and leaves creates no factor.</li>
 *   <li><b>Show.</b> The QR code, with the setup key as the fallback for a phone that cannot scan.</li>
 *   <li><b>Verify.</b> {@link TotpEnrolmentService#confirm} checks a code against the pending seed and
 *       only then creates the factor. A wrong code creates nothing: the factor is proven before it
 *       exists, not after.</li>
 *   <li><b>Recovery codes.</b> Issued in the same breath, because this is the only moment the user is
 *       guaranteed to be looking. Acknowledging the sheet is what returns them to the settings page.</li>
 * </ol>
 *
 * <p><b>The pending enrolment lives in this UI instance, not the session.</b> It is only needed while
 * the page is on screen, and the service's own {@link TotpEnrolmentService#ENROLMENT_WINDOW} bounds an
 * abandoned one anyway. Keeping it out of the session also means a refresh starts a fresh seed rather
 * than rendering a QR code whose seed nobody can find again.</p>
 *
 * @since 1.0.0
 */
@Route(value = "settings/two-factor/enrol", layout = SettingsMainLayout.class)
@PageTitle("Settings · Set up an authenticator app")
@SpringComponent
@UIScope
@PermitAll
public class TwoFactorEnrolmentMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Route segment, so callers navigate by constant rather than by literal. */
  public static final String PATH = "settings/two-factor/enrol";

  /** The label an account gets when the user does not choose one. */
  public static final String DEFAULT_DISPLAY_NAME = TwoFactorSettingsCopy.DEFAULT_FACTOR_NAME;

  /**
   * How long a started enrolment may be confirmed.
   *
   * <p>Mirrors {@code TotpEnrolmentService.ENROLMENT_WINDOW}; the service is the authority and still
   * refuses an expired confirmation. This copy exists so the page can disable the action before the
   * user types a code that cannot succeed.</p>
   */
  static final java.time.Duration ENROLMENT_WINDOW = java.time.Duration.ofMinutes(15);

  static final String NAME_LABEL = TwoFactorSettingsCopy.ENROL_NAME_LABEL;

  /** Text of the breadcrumb back to the settings page; names the destination, not "Back". */
  static final String BACK_LINK_TEXT = "Two-factor authentication";

  /** The shared back-link class, so this page cannot drift from the group sub-pages. */
  static final String BACK_LINK_CSS_CLASS = "settings-back-link";
  static final String NAME_CSS_CLASS = "mfa-enrol-name";
  static final String CONFIRM_LABEL = "Verify and enable";

  /**
   * The visual order of the steps, as the user performs them.
   *
   * <p>Kept as data because it was wrong once already: the dialog had the optional name <em>above</em>
   * the code, so reading top to bottom asked for a nickname before asking for the one thing that
   * finishes enrolment. Pinned by {@code EnrolmentRouteSpec}.</p>
   *
   * @return the step keys in visual order
   */
  static List<String> stepOrder() {
    return List.of("qr", "setup-key", "code", "name");
  }

  private final transient TotpEnrolmentService enrolmentService;
  private final transient SecondFactorService secondFactorService;
  private final transient RecoveryCodeService recoveryCodeService;
  private final transient UserInformationService userInformationService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  private final TextField code = new TextField(TwoFactorSettingsCopy.ENROL_CODE_LABEL);
  private final TextField displayName = new TextField(NAME_LABEL);
  private final Span error = new Span();
  private final Span sessionNotice = new Span();
  private final Div content = new Div();
  /**
   * The way back, as a breadcrumb above the section — the same shape as {@code GroupDetailMain} and
   * {@code AdminGroupManagersMain}, which is how this application does back navigation on settings
   * sub-pages.
   */
  private final RouterLink backLink =
      new RouterLink(BACK_LINK_TEXT, TwoFactorAuthenticationMain.class);

  /** Started on entry; see the class documentation for why it is not in the session. */
  private transient PendingTotpEnrolment pending;

  public TwoFactorEnrolmentMain(TotpEnrolmentService enrolmentService,
      SecondFactorService secondFactorService,
      RecoveryCodeService recoveryCodeService,
      UserInformationService userInformationService,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.enrolmentService = requireNonNull(enrolmentService,
        "enrolmentService must not be null");
    this.secondFactorService = requireNonNull(secondFactorService,
        "secondFactorService must not be null");
    this.recoveryCodeService = requireNonNull(recoveryCodeService,
        "recoveryCodeService must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");

    addClassName("two-factor-enrolment");
    content.addClassName("mfa-enrol-body");
    backLink.addClassName(BACK_LINK_CSS_CLASS);
    backLink.setId("enrolment-back");
    // The breadcrumb sits above the section, matching the group pages: the settings aside already
    // highlights "Two-factor", and this link says which page of it you came from.
    add(backLink);
    add(content);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // A page, so it can be deep-linked, bookmarked or reloaded — start a fresh enrolment on each
    // entry rather than rendering a QR code whose pending state is gone.
    String userId = currentUserId();
    UserInfo user = userInformationService.findById(userId)
        .orElseThrow(() -> new IllegalStateException(
            "the signed-in account could not be loaded"));
    pending = enrolmentService.begin(userId, DEFAULT_DISPLAY_NAME, user.emailAddress(),
        Instant.now());
    render(user.emailAddress());
  }

  private void render(String accountEmail) {
    content.removeAll();

    SettingsSection section = new SettingsSection(TwoFactorSettingsCopy.ENROL_TITLE,
        TwoFactorSettingsCopy.ENROL_INTRO);

    // QR beside the code, not stacked above it. Side by side, the two things the user must do —
    // scan, then type — are visible without scrolling, which is what a stacked layout could not
    // achieve inside a dialog and still cannot quite achieve on a short window.
    Div qrBlock = new Div(new TotpQrCode(pending.provisioningUri()));
    qrBlock.addClassName("mfa-qr-block");

    code.setLabel(TwoFactorSettingsCopy.ENROL_CODE_LABEL);
    code.setWidthFull();
    code.setMaxLength(Totp.DIGITS);
    code.setAllowedCharPattern("\\d");
    code.setAutofocus(true);
    code.setPlaceholder("123456");

    Button verify = new Button(TwoFactorSettingsCopy.ENROL_CONFIRM_LABEL);
    verify.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    verify.addClickShortcut(Key.ENTER);
    verify.addClickListener(event -> verifyCode(accountEmail));
    verify.setId("verify-enrolment");

    error.addClassName("mfa-error");
    error.getStyle().set("color", "var(--lumo-error-text-color)");
    error.setVisible(false);

    Div verifyColumn = new Div(code, verify, error);
    verifyColumn.addClassName("mfa-enrol-verify");

    Div pair = new Div(qrBlock, verifyColumn);
    pair.addClassName("mfa-enrol-pair");

    // The manual key sits under the pair because it is the fallback path, not a step of its own.
    TextField setupKey = new TextField(TwoFactorSettingsCopy.ENROL_SETUP_KEY_LABEL);
    setupKey.setValue(setupKeyFrom(pending.provisioningUri()));
    setupKey.setReadOnly(true);
    setupKey.setWidthFull();
    CopyToClipboardButton copy = new CopyToClipboardButton("Copy");
    copy.addClickListener(event -> copy.copy(setupKey.getValue()));
    Div setupKeyRow = new Div(setupKey, copy);
    setupKeyRow.addClassName("mfa-setup-key-row");

    // Optional and last: it defaults, and it can be changed later from the list, so it must not stand
    // between the user and the two things they actually have to do.
    displayName.setLabel(TwoFactorSettingsCopy.ENROL_NAME_LABEL);
    displayName.setValue(DEFAULT_DISPLAY_NAME);
    displayName.setMaxLength(SecondFactor.MAX_DISPLAY_NAME_LENGTH);
    displayName.setHelperText(TwoFactorSettingsCopy.ENROL_NAME_HELPER);
    displayName.setClearButtonVisible(true);
    displayName.addClassName(NAME_CSS_CLASS);

    // Cancel is a secondary button in the content flow. The way *back* is the breadcrumb at the top,
    // which is how every other settings sub-page in this application does it.
    Button cancel = new Button("Cancel",
        event -> UI.getCurrent().navigate(TwoFactorAuthenticationMain.class));
    Div actions = new Div(cancel);
    actions.addClassName("mfa-enrol-actions");

    sessionNotice.setVisible(false);
    sessionNotice.addClassName("mfa-enrol-session-notice");

    section.addContent(pair, setupKeyRow, displayName, sessionNotice, actions);
    content.add(section);
  }

  private void verifyCode(String accountEmail) {
    if (pending == null) {
      return;
    }
    error.setVisible(false);
    code.setInvalid(false);
    String presented = code.getValue() == null ? "" : code.getValue().strip();
    if (presented.isEmpty()) {
      showError("Enter the six-digit code shown by the authenticator app.");
      return;
    }

    if (expired()) {
      // Checked against the same window the service enforces, so the page cannot disagree with it.
      // Without this the user types a valid code into a setup that has quietly timed out (MFA-35's
      // neighbourhood: a long-open page must not look live).
      showError("This setup has expired. Reload the page to get a fresh code.");
      gameOver();
      return;
    }

    SecondFactor created;
    try {
      created = enrolmentService.confirm(pending, presented, Instant.now());
    } catch (InvalidTotpCodeException e) {
      // The one case worth advising on: a phone whose clock has drifted is the usual cause.
      showError("That code is not valid. Check that the phone's clock is set automatically, "
          + "then try the current code.");
      return;
    } catch (EnrolmentExpiredException e) {
      showError("This setup took too long. Start again to get a fresh code.");
      return;
    } catch (SecondFactorSecretUnavailableException e) {
      // Fail closed and say so: the alternative is silently treating setup as done.
      showError("Second-factor setup is temporarily unavailable. Please try again later.");
      return;
    }

    renameIfChosen(created);

    // Issue a sheet ONLY if the account has none left. A sheet is destructive: generating retires
    // every existing code in the same transaction, so issuing one on every enrolment would kill the
    // codes a redundancy-minded user printed when they added their second app. That is a regression
    // this page introduced — the dialog-based flow had the guard and the conversion dropped it.
    String userId = pending.userId();
    long unusedCodes = recoveryCodeService.remaining(userId);
    if (!TwoFactorSettingsCopy.recoverySheetNeededOnEnrolment(unusedCodes)) {
      showEnrolledWithoutNewCodes(accountEmail, unusedCodes > 0);
      return;
    }

    var codeSet = recoveryCodeService.generate(userId, Instant.now());
    String chosenEmail = accountEmail;
    RecoveryCodesDialog sheet = new RecoveryCodesDialog(codeSet, chosenEmail,
        RecoveryCodesDialog.Reason.FIRST_FACTOR);
    // Recorded before the sheet opens and cleared only by the acknowledgement, so a refresh loses the
    // sheet loudly rather than silently (MFA-91). Same marker the settings page reads.
    RecoveryCodeAcknowledgement.markUnacknowledged(userId, true);
    sheet.addCodesSavedListener(event -> {
      RecoveryCodeAcknowledgement.markUnacknowledged(userId, false);
      UI.getCurrent().navigate(TwoFactorAuthenticationMain.class);
    });
    sheet.open();
  }

  /**
   * Applies the optional label, in the same step as the confirmation.
   *
   * <p>Name first would enrol under a label nobody asked for if the code fails; code first and name
   * after would leave a window in which the factor exists under the default. So the factor is created
   * and relabelled together. A rename that fails cannot hide the enrolment, so the default stands and
   * the failure is swallowed the same way the settings page does it.</p>
   */
  private void renameIfChosen(SecondFactor created) {
    String typed = displayName.getValue() == null ? "" : displayName.getValue().strip();
    String name = TwoFactorSettingsCopy.factorName(typed);
    if (name.isEmpty() || name.equals(created.displayName())) {
      return;
    }
    secondFactorService.rename(created.userId(), created.id(), name);
  }

  private boolean expired() {
    return Instant.now().isAfter(pending.createdAt().plus(ENROLMENT_WINDOW));
  }

  /** Disables the action that can no longer succeed, so the page stops inviting it. */
  private void gameOver() {
    code.setEnabled(false);
    sessionNotice.setText("Reload the page to start a fresh enrolment.");
    sessionNotice.setVisible(true);
  }

  /**
   * Confirms the enrolment and says the saved codes are untouched.
   *
   * <p>Replaces the page content rather than navigating straight back: the user needs one sentence
   * confirming that adding this app did not invalidate the codes they already have, and then a way
   * back. Navigating immediately would leave them wondering whether they just lost their sheet.
   * </p>
   */
  private void showEnrolledWithoutNewCodes(String accountEmail, boolean hasUnusedCodes) {
    content.removeAll();

    // Headed for what happened, not for what was being done: the page said "Set up an authenticator
    // app" one second before the app existed, which reads as unfinished work.
    SettingsSection section = new SettingsSection(TwoFactorSettingsCopy.ENROL_DONE_TITLE);

    // The shared success component, so this state looks like every other confirmation in the
    // application instead of like a paragraph that happened to land here.
    SuccessMessage confirmation = new SuccessMessage(
        "Two-factor authentication is on for this account",
        TwoFactorSettingsCopy.enrolmentDoneWithoutNewCodesBody(hasUnusedCodes));

    // A breadcrumb-shaped way back, not a full-width primary bar: the flow is finished, and there is
    // no next step for a primary button to invite.
    RouterLink done = new RouterLink(TwoFactorSettingsCopy.ENROL_DONE_ACTION,
        TwoFactorAuthenticationMain.class);
    done.addClassName("mfa-enrol-done-link");
    done.setId("enrolment-done");

    section.addContent(confirmation, done);
    content.add(section);
  }

  private void showError(String message) {
    error.setText(message);
    error.setVisible(true);
    code.setInvalid(true);
  }

  private String currentUserId() {
    return userIdTranslator
        .translateToUserId(SecurityContextHolder.getContext().getAuthentication())
        .orElseThrow(() -> new IllegalStateException("no signed-in account on the enrolment page"));
  }

  /**
   * Reads the shared key out of the {@code otpauth://} URI.
   *
   * <p>The URI is the domain's interface for "what the authenticator needs"; the manual-entry field is
   * a second rendering of it. Same helper as {@code RequiredEnrolmentView}, deliberately duplicated in
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
}
