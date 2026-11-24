package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
import life.qbic.identity.application.mfa.PendingTotpEnrolment;
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

  private final transient TwoFactorAuthenticationComponent component;
  private final transient SecondFactorService secondFactorService;
  private final transient TotpEnrolmentService totpEnrolmentService;
  private final transient RecoveryCodeService recoveryCodeService;
  private final transient MfaPolicy mfaPolicy;
  private final transient MfaGraceRepository graceRepository;
  private final transient UserInformationService userInformationService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  public TwoFactorAuthenticationMain(SecondFactorService secondFactorService,
      TotpEnrolmentService totpEnrolmentService,
      RecoveryCodeService recoveryCodeService,
      MfaPolicy mfaPolicy,
      MfaGraceRepository graceRepository,
      UserInformationService userInformationService,
      AuthenticationToUserIdTranslationService userIdTranslator,
      TwoFactorAuthenticationComponent component) {
    this.secondFactorService = requireNonNull(secondFactorService,
        "secondFactorService must not be null");
    this.totpEnrolmentService = requireNonNull(totpEnrolmentService,
        "totpEnrolmentService must not be null");
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

  private void onEnrolRequested() {
    String userId = currentUserId();
    var user = userInformationService.findById(userId).orElseThrow();
    var pending = totpEnrolmentService.begin(userId, TotpEnrolmentDialog.DEFAULT_DISPLAY_NAME,
        user.emailAddress(), Instant.now());
    // The verification callback is passed in rather than the service, so the dialog cannot create a
    // factor by itself; "no factor without a verified code" stays a property of the application layer.
    var dialog = new TotpEnrolmentDialog(pending,
        (code, chosenName) -> enrolWithName(userId, pending, code, chosenName));
    dialog.addEnrolledListener(event -> onEnrolled(userId, user.emailAddress()));
    dialog.open();
  }

  /**
   * Confirms the enrolment and stores the chosen name in the same transaction.
   *
   * <p>Name first would be a factor that exists under a label nobody asked for if the code fails; code
   * first and name after would leave a window in which the factor is enrolled under the default. So the
   * factor is created and relabelled together, under the application service's transaction.</p>
   *
   * @param userId     the account enrolling
   * @param pending    the enrolment started earlier
   * @param code       the six-digit code from the app
   * @param chosenName what the user called the app; may be blank, in which case the default stands
   * @return the stored factor
   */
  private SecondFactor enrolWithName(String userId, PendingTotpEnrolment pending, String code,
      String chosenName) {
    SecondFactor created = totpEnrolmentService.confirm(pending, code, Instant.now());
    String name = chosenName == null ? "" : chosenName.strip();
    if (name.isEmpty() || name.equals(created.displayName())) {
      return created;
    }
    // The factor exists and the code verified; a rename cannot fail in a way that should hide the
    // enrolment, so an unexpected failure is logged and the default label stands.
    return secondFactorService.rename(userId, created.id(), name).orElse(created);
  }

  /**
   * What happens the moment the first factor is confirmed.
   *
   * <p>The recovery codes are issued <b>here</b>, in the same breath as the factor, because this is
   * the only moment at which the user is guaranteed to be paying attention and to have just proved
   * both password and a fresh device. Leaving it as a second, optional click on the page is how
   * people end up enrolled with no recovery codes — and then a lost phone becomes an identity
   * verification at the helpdesk, not a self-service recovery.</p>
   *
   * <p>A fresh set is only issued when the account has none left, so adding a second device does not
   * silently invalidate a sheet the user has already printed.</p>
   *
   * @param userId       the account that just enrolled
   * @param accountEmail the account, named on the sheet so it is identifiable later
   */
  private void onEnrolled(String userId, String accountEmail) {
    refresh();
    if (recoveryCodeService.remaining(userId) > 0) {
      return;
    }
    var codeSet = recoveryCodeService.generate(userId, Instant.now());
    refresh();
    new RecoveryCodesDialog(codeSet, accountEmail, RecoveryCodesDialog.Reason.FIRST_FACTOR).open();
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
      new RecoveryCodesDialog(codeSet, accountEmail, RecoveryCodesDialog.Reason.REGENERATED).open();
      refresh();
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
