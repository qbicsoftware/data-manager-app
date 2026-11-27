package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.DialogAction;
import life.qbic.datamanager.views.settings.SettingsMainLayout;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.mfa.MfaPolicy;
import life.qbic.identity.application.mfa.RecoveryCodeService;
import life.qbic.identity.application.mfa.SecondFactorService;
import life.qbic.identity.application.mfa.TotpEnrolmentService;
import life.qbic.identity.domain.model.mfa.MfaGrace;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.model.mfa.SecondFactor;
import life.qbic.identity.domain.model.mfa.SecondFactorRemoval;
import life.qbic.identity.domain.repository.MfaGraceRepository;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Two-factor authentication settings page</b>
 *
 * <p>Route: {@code /settings/two-factor}, hosted by {@link SettingsMainLayout}.</p>
 *
 * <p>The page owns the three flows and their transactions: enrol, remove and generate recovery codes.
 * The component it hosts renders state and raises intent, so all account-scoped decisions — whose
 * factors are listed, who may remove one, when a new code set retires the old — happen here and in the
 * application services, not in the view tree.</p>
 *
 * <p>Removal and regeneration are destructive and therefore always confirmed, with the consequence
 * spelled out rather than a generic "are you sure": losing your last factor without a recovery code
 * means an administrator has to re-enrol you (M6-1).</p>
 *
 * @since 1.0.0
 */
@Route(value = "settings/two-factor", layout = SettingsMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
@PageTitle("Settings · Two-factor authentication")
public class TwoFactorAuthenticationMain extends Main implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Session marker: a recovery-code sheet was shown and not acknowledged.
   *
   * <p>In the session rather than in this component because the component does not survive the event
   * that loses the codes. A browser refresh destroys the dialog and the UI instance with it, so a
   * field on the page would be gone by the time the page is rendered again — the marker has to outlive
   * the render that follows (MFA-91).</p>
   */
  static final String LOST_RECOVERY_CODES_ATTRIBUTE =
      RecoveryCodeAcknowledgement.UNACKNOWLEDGED_ATTRIBUTE;

  private final transient TwoFactorAuthenticationComponent component;
  private final transient SecondFactorService secondFactorService;
  private final transient RecoveryCodeService recoveryCodeService;
  private final transient MfaPolicy mfaPolicy;
  private final transient MfaGraceRepository graceRepository;
  private final transient UserInformationService userInformationService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  public TwoFactorAuthenticationMain(SecondFactorService secondFactorService,
      RecoveryCodeService recoveryCodeService,
      MfaPolicy mfaPolicy,
      MfaGraceRepository graceRepository,
      UserInformationService userInformationService,
      AuthenticationToUserIdTranslationService userIdTranslator,
      TwoFactorAuthenticationComponent component) {
    this.secondFactorService = requireNonNull(secondFactorService,
        "secondFactorService must not be null");
    this.recoveryCodeService = requireNonNull(recoveryCodeService,
        "recoveryCodeService must not be null");
    this.mfaPolicy = requireNonNull(mfaPolicy, "mfaPolicy must not be null");
    this.graceRepository = requireNonNull(graceRepository, "graceRepository must not be null");
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");

    this.component = requireNonNull(component, "component must not be null");
    addClassName("two-factor-authentication");
    add(component);
    component.addEnrolListener(event -> onEnrolRequested());
    component.addRemoveListener(event -> onRemoveRequested(event.factorId()));
    component.addRenameListener(event -> onRenameRequested(event.factorId()));
    component.addGenerateRecoveryCodesListener(event -> onGenerateRecoveryCodesRequested());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    // The navigation hides this entry under OFF, but a deep link, a bookmark or an old e-mail would
    // still reach the route. Under OFF nothing is asked at sign-in, so this page could only offer work
    // that has no effect — it is not a page, so send the user to the settings hub's first section.
    if (!SecondFactorRemoval.twoFactorCanBeEnabled(mfaPolicy.mode())) {
      event.rerouteTo(UserProfileMain.class);
      return;
    }
    refresh();
  }

  /**
   * Renders the account's state, and says when enforcement will start biting.
   *
   * <p>The deadline is read here rather than in the component because it is persistence: the page
   * owns the transaction and the services, the component only renders. Passing it in keeps
   * {@link TwoFactorSettingsCopy} a pure function of its inputs, which is what makes the copy
   * testable.</p>
   */
  private void refresh() {
    String userId = currentUserId();
    var factors = secondFactorService.activeFactors(userId);
    // The outcome per factor comes from the same decision the removal itself uses, so the button
    // state and the server behaviour cannot drift apart.
    Map<Long, SecondFactorRemoval.Outcome> outcomes = factors.stream().collect(
        Collectors.toMap(SecondFactor::id,
            factor -> secondFactorService.removalOutcome(userId, factor.id())));
    component.setFactors(factors, outcomes);
    component.setRecoveryCodeCount(recoveryCodeService.remaining(userId));
    component.setPolicyMode(mfaPolicy.mode());
    component.setRecoveryCodesLost(isRecoverySheetUnacknowledged());
    // Only meaningful under REQUIRED without a factor: an unenrolled account is on a deadline and the
    // page has to name it, or "until the grace period ends" tells the user nothing they can act on.
    if (mfaPolicy.mode() == MfaPolicyMode.REQUIRED && factors.isEmpty()) {
      component.setGraceDeadline(graceRepository.findByUserId(userId)
          .map(MfaGrace::graceUntil)
          .orElse(null));
    } else {
      component.setGraceDeadline(null);
    }
  }

  /**
   * Opens a one-time sheet and remembers whether it was acknowledged.
   *
   * <p>The marker is set <em>before</em> the dialog opens and cleared only by the acknowledgement, so
   * every other exit — refresh, navigation, a closed browser tab — leaves it standing and the next
   * render reports the loss. Nothing else can detect it: the plaintext codes are not stored anywhere
   * (AUTH-R-03), so "was it saved?" is not a question the database can answer.</p>
   *
   * @param dialog the sheet to open
   */
  private void showRecoveryCodes(RecoveryCodesDialog dialog) {
    markRecoverySheetUnacknowledged(true);
    dialog.addCodesSavedListener(event -> markRecoverySheetUnacknowledged(false));
    dialog.open();
  }

  private void markRecoverySheetUnacknowledged(boolean unacknowledged) {
    RecoveryCodeAcknowledgement.markUnacknowledged(currentUserId(), unacknowledged);
  }

  private boolean isRecoverySheetUnacknowledged() {
    return RecoveryCodeAcknowledgement.isUnacknowledged();
  }

  /**
   * Sends the user to the enrolment page.
   *
   * <p>Enrolment used to be a modal. It carried a QR code, a setup key and two inputs, and its footer
   * fell below the fold at ordinary window sizes; that content is taller than a dialog should be, and
   * multi-step creation lives on routes everywhere else in this application — including mandatory
   * enrolment ({@code RequiredEnrolmentView}). The page also scrolls, which is the fix for MFA-93
   * rather than re-tuning a modal until it fits one viewport.</p>
   */
  private void onEnrolRequested() {
    UI.getCurrent().navigate(TwoFactorEnrolmentMain.class);
  }

  private void onRemoveRequested(long factorId) {
    String userId = currentUserId();
    SecondFactorRemoval.Outcome outcome = secondFactorService.removalOutcome(userId, factorId);

    if (outcome == SecondFactorRemoval.Outcome.TURNS_OFF_TWO_FACTOR) {
      // Not a removal: this is the account switching two-factor authentication off, and it destroys
      // the only reset-free path with it. The consequence is spelled out rather than implied.
      AlertDialog.danger(this,
          "Turn off two-factor authentication?",
          "Your authenticator app will be removed and all recovery codes will be deleted. "
              + "After that, your password alone gets into this account.",
          "Turn it off",
          "Keep it on",
          () -> removeAndRefresh(factorId)).open();
      return;
    }

    AlertDialog.danger(this,
        "Remove this authenticator app?",
        "If this is your last one and you have no recovery codes, an administrator will have to "
            + "re-enrol you before you can sign in again.",
        "Remove it",
        "Keep it",
        () -> removeAndRefresh(factorId)).open();
  }

  /**
   * Opens the rename dialog for one factor.
   *
   * <p>A one-field dialog rather than inline editing: the factor list is a read surface, and turning a
   * card into a form to change one word makes every other card look editable too.</p>
   *
   * @param factorId the factor to relabel
   */
  private void onRenameRequested(long factorId) {
    String userId = currentUserId();
    Optional<SecondFactor> factor = secondFactorService.activeFactors(userId).stream()
        .filter(candidate -> candidate.id() == factorId)
        .findFirst();
    if (factor.isEmpty()) {
      // Removed in another tab, or never the caller's. Reload rather than show a dialog for a factor
      // that is gone.
      refresh();
      return;
    }
    new RenameFactorDialog(factor.get().displayName(), newName -> {
      try {
        secondFactorService.rename(userId, factorId, newName);
      } catch (IllegalArgumentException e) {
        // The domain refuses a blank or over-long label; the dialog validates first, so this is a
        // backstop for a hand-crafted request rather than a path a user reaches.
        return false;
      }
      refresh();
      return true;
    }).open();
  }

  private void removeAndRefresh(long factorId) {
    secondFactorService.remove(currentUserId(), factorId, Instant.now());
    refresh();
  }

  private void onGenerateRecoveryCodesRequested() {
    String userId = currentUserId();
    // Second line of defence: the page disables the action while the account has no factor, because
    // a code set generated then could never be redeemed (no factor means no challenge). A client that
    // ignores the disabled state must not get a one-time sheet it cannot use either.
    if (secondFactorService.activeFactors(userId).isEmpty()) {
      return;
    }
    long remaining = recoveryCodeService.remaining(userId);
    DialogAction generate = () -> {
      var codeSet = recoveryCodeService.generate(userId, Instant.now());
      // The sheet names the account it belongs to, so a sheet found later is still assignable — and a
      // user with two accounts (a shared device, a second lab) has no other way to tell them apart.
      String accountEmail = userInformationService.findById(userId).orElseThrow().emailAddress();
      // Regenerated on purpose, so the dialog may be dismissed: the user has been here before and
      // knows what the sheet is. The first-factor sheet is the one that cannot be waved away.
      // Generation retires the previous set, so a lost sheet it replaces is no longer news: the
      // user has new codes to save, and the notice is cleared with this flow's own acknowledgement.
      refresh();
      showRecoveryCodes(
          new RecoveryCodesDialog(codeSet, accountEmail, RecoveryCodesDialog.Reason.REGENERATED));
    };
    if (remaining == 0) {
      generate.execute();
      return;
    }
    AlertDialog.danger(this,
        "Generate a new set of recovery codes?",
        "Your " + (remaining == 1 ? "existing unused code" : remaining + " existing unused codes")
            + " will stop working immediately. Codes you have already used stay used.",
        "Generate new codes",
        "Keep the current codes",
        generate).open();
  }

  private String currentUserId() {
    return userIdTranslator.translateToUserId(
            SecurityContextHolder.getContext().getAuthentication())
        .orElseThrow();
  }
}
