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
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.DialogAction;
import life.qbic.datamanager.views.settings.SettingsMainLayout;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.application.mfa.MfaPolicy;
import life.qbic.identity.application.mfa.RecoveryCodeService;
import life.qbic.identity.application.mfa.SecondFactorService;
import life.qbic.identity.application.mfa.TotpEnrolmentService;
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
  private final transient UserInformationService userInformationService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  public TwoFactorAuthenticationMain(SecondFactorService secondFactorService,
      TotpEnrolmentService totpEnrolmentService,
      RecoveryCodeService recoveryCodeService,
      MfaPolicy mfaPolicy,
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
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");

    this.component = requireNonNull(component, "component must not be null");
    addClassName("two-factor-authentication");
    add(component);
    component.addEnrolListener(event -> onEnrolRequested());
    component.addRemoveListener(event -> onRemoveRequested(event.factorId()));
    component.addGenerateRecoveryCodesListener(event -> onGenerateRecoveryCodesRequested());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    refresh();
  }

  private void refresh() {
    String userId = currentUserId();
    component.setFactors(secondFactorService.activeFactors(userId));
    component.setRecoveryCodeCount(recoveryCodeService.remaining(userId));
    component.setPolicyMode(mfaPolicy.mode());
  }

  private void onEnrolRequested() {
    String userId = currentUserId();
    var user = userInformationService.findById(userId).orElseThrow();
    var pending = totpEnrolmentService.begin(userId, "Authenticator app", user.emailAddress(),
        Instant.now());
    // The verification callback is passed in rather than the service, so the dialog cannot create a
    // factor by itself; "no factor without a verified code" stays a property of the application layer.
    var dialog = new TotpEnrolmentDialog(pending,
        code -> totpEnrolmentService.confirm(pending, code, Instant.now()));
    dialog.addEnrolledListener(event -> refresh());
    dialog.open();
  }

  private void onRemoveRequested(long factorId) {
    AlertDialog.danger(this,
        "Remove this authenticator app?",
        "If this is your last one and you have no recovery codes, an administrator will have to "
            + "re-enrol you before you can sign in again.",
        "Remove it",
        "Keep it",
        () -> {
          secondFactorService.disable(currentUserId(), factorId, Instant.now());
          refresh();
        }).open();
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
      new RecoveryCodesDialog(codeSet, accountEmail).open();
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
