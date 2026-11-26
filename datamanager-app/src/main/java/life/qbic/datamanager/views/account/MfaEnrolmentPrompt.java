package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import life.qbic.identity.application.mfa.MfaPolicy;
import life.qbic.identity.application.mfa.SecondFactorService;
import life.qbic.identity.domain.model.mfa.MfaGrace;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.repository.MfaGraceRepository;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>The in-app prompt that reaches accounts which never open the settings page (M7-2)</b>
 *
 * <p>Enrolment used to be reachable only by someone who already knew it existed: the settings hub
 * hides the two-factor entry under {@code OFF}, and under {@code OPTIONAL} nothing anywhere in the
 * application mentions the feature. A user who never visits that page therefore never learns that a
 * second factor is available — and under {@code REQUIRED} they would learn it only when the grace
 * window runs out, i.e. at the lockout.</p>
 *
 * <p>This banner closes that gap. It is mounted in the application shell
 * ({@code DataManagerLayout}), so it appears on every authenticated page, and it renders only when
 * all three of these hold (see {@link TwoFactorSettingsCopy#promptVisible}):</p>
 * <ol>
 *   <li>the deployment asks for a factor at all — under {@code OFF} the prompt would send the user to
 *       a page that reroutes away, because nothing is asked at sign-in;</li>
 *   <li>the account has no active factor — an enrolled account has nothing to do here;</li>
 *   <li>the user has not dismissed it in this session.</li>
 * </ol>
 *
 * <p><b>Dismissal is per session, not per account.</b> Persisting it would need a column and a
 * migration for a preference that is not worth one, and the honest reading of "not now" is "not in
 * this sitting". Under {@code REQUIRED} there is no dismissal at all: an offer may be declined, a
 * deadline may not be hidden.</p>
 *
 * <p><b>One query per navigation, and only when it can matter.</b> The factor count is read on
 * attach, after the mode check, so a deployment with two-factor switched off pays nothing. The
 * alternative — caching the answer in the session — would have to be invalidated by the settings
 * page on every enrolment and removal, and a stale cache here means a prompt that lies.</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class MfaEnrolmentPrompt extends Div implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Session key: the user said "not now" in this sitting. */
  static final String DISMISSED_ATTRIBUTE = MfaEnrolmentPrompt.class.getName() + ".dismissed";

  /**
   * Session key: how many recovery codes were left when the last one was spent (MFA-41b).
   *
   * <p>Set at the login challenge, read here. It cannot be a message on the challenge page itself:
   * redeeming a code establishes the session and redirects, so anything written into that page's
   * components is discarded one instruction later. The prompt is mounted in the application shell, so
   * it survives the redirect and is the first thing the user sees after signing in.</p>
   *
   * <p>Stored as the count (typically {@code 0}) rather than a flag so the sentence can be specific,
   * and removed on read so it is shown once rather than on every navigation.</p>
   */
  public static final String RECOVERY_CODES_LEFT_ATTRIBUTE =
      MfaEnrolmentPrompt.class.getName() + ".recovery-codes-left";

  // Deliberately gone: this used to hold its own date-only formatter, which is how the banner came to
  // say "on 28 November 2026" while the settings page could say something slightly different. The
  // format now lives in TwoFactorSettingsCopy with the other deadline copy.

  private final transient MfaPolicy mfaPolicy;
  private final transient SecondFactorService secondFactorService;
  private final transient MfaGraceRepository graceRepository;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  private final Span title = new Span();
  private final Span body = new Span();
  private final Button enrol = new Button("Set up authenticator app");
  private final Button dismiss = new Button("Not now");

  public MfaEnrolmentPrompt(MfaPolicy mfaPolicy, SecondFactorService secondFactorService,
      MfaGraceRepository graceRepository,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.mfaPolicy = requireNonNull(mfaPolicy, "mfaPolicy must not be null");
    this.secondFactorService = requireNonNull(secondFactorService,
        "secondFactorService must not be null");
    this.graceRepository = requireNonNull(graceRepository, "graceRepository must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator, "userIdTranslator must not be null");

    addClassName("mfa-prompt");
    setVisible(false);

    Icon shield = VaadinIcon.SHIELD.create();
    shield.addClassName("mfa-prompt__icon");
    title.addClassName("mfa-prompt__title");
    body.addClassName("mfa-prompt__body");
    Div text = new Div(title, body);
    text.addClassName("mfa-prompt__text");

    enrol.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
    dismiss.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
    Div actions = new Div(enrol, dismiss);
    actions.addClassName("mfa-prompt__actions");

    add(shield, text, actions);

    enrol.addClickListener(event -> UI.getCurrent().navigate(TwoFactorAuthenticationMain.class));
    dismiss.addClickListener(event -> {
      VaadinSession.getCurrent().setAttribute(DISMISSED_ATTRIBUTE, Boolean.TRUE);
      setVisible(false);
    });
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    refresh();
  }

  /**
   * Decides whether this account sees the prompt, and with which words.
   *
   * <p>Called on every attach, i.e. on every navigation. It returns early for the two cheap cases
   * (mode {@code OFF}, already dismissed) before touching the database.</p>
   */
  private void refresh() {
    MfaPolicyMode mode = mfaPolicy.mode();
    boolean dismissed = Boolean.TRUE.equals(
        VaadinSession.getCurrent().getAttribute(DISMISSED_ATTRIBUTE));

    // The recovery-code warning outranks everything: it is a consequence of an action the user just
    // took, and — unlike the enrolment offer — it concerns an account that already has a factor.
    if (showRecoveryCodesLeft()) {
      return;
    }

    if (mode == MfaPolicyMode.OFF || dismissed || onTwoFactorPage()) {
      setVisible(false);
      return;
    }

    String userId = userIdTranslator
        .translateToUserId(SecurityContextHolder.getContext().getAuthentication())
        .orElse(null);
    if (userId == null) {
      // Anonymous or an unexpected principal: the shell is also used by the login pages.
      setVisible(false);
      return;
    }

    boolean hasFactor = !secondFactorService.activeFactors(userId).isEmpty();
    if (!TwoFactorSettingsCopy.promptVisible(mode, hasFactor, dismissed)) {
      setVisible(false);
      return;
    }

    title.setText(TwoFactorSettingsCopy.promptTitle(mode));
    body.setText(TwoFactorSettingsCopy.promptBody(mode, deadlineFor(userId, mode)));
    dismiss.setVisible(TwoFactorSettingsCopy.promptDismissible(mode));
    setVisible(true);
  }

  /**
   * Reports the recovery-code count left by the challenge, once, and returns whether it did.
   *
   * <p>Two shapes: the last code was just spent (the state that turns a lost phone into a helpdesk
   * ticket), or some remain and the count is the user's only way to judge how much time they have.
   * The attribute is consumed either way — a notice about an event must not become a permanent banner.</p>
   *
   * @return {@code true} if a notice was rendered
   */
  private boolean showRecoveryCodesLeft() {
    VaadinSession session = VaadinSession.getCurrent();
    if (session == null) {
      return false;
    }
    Object value = session.getAttribute(RECOVERY_CODES_LEFT_ATTRIBUTE);
    if (!(value instanceof Integer left)) {
      return false;
    }
    session.setAttribute(RECOVERY_CODES_LEFT_ATTRIBUTE, null);

    if (onTwoFactorPage()) {
      // The settings page states the count itself, one line above the action that fixes it.
      setVisible(false);
      return true;
    }

    boolean noneLeft = left <= 0;
    // Every sentence this page can show is decided in TwoFactorSettingsCopy, so the event notice is
    // too — including the one that must never mention e-mail.
    title.setText(TwoFactorSettingsCopy.recoveryCodesLeftTitle(left));
    body.setText(TwoFactorSettingsCopy.recoveryCodesLeftBody(left));
    // Not dismissible: this is the account's own safety net running out, not an offer.
    dismiss.setVisible(false);
    enrol.setVisible(true);
    enrol.setText(noneLeft ? "Set up an authenticator app" : "Manage two-factor authentication");
    setVisible(true);
    return true;
  }

  /**
   * The grace deadline, when there is one to name.
   *
   * <p>Only {@code REQUIRED} has a deadline, and only for an account without a factor. An absent row
   * means "a window will be granted at the next login" ({@code MfaGate}), not "no deadline", so the
   * prompt then states the requirement without a date rather than inventing one.</p>
   */
  private String deadlineFor(String userId, MfaPolicyMode mode) {
    if (mode != MfaPolicyMode.REQUIRED) {
      return null;
    }
    return graceRepository.findByUserId(userId)
        .map(MfaGrace::graceUntil)
        .map(TwoFactorSettingsCopy::formattedDeadline)
        .orElse(null);
  }

  /**
   * The prompt is redundant on the page that exists to fix it, and on that page it would push the
   * actual content down for no reason.
   */
  private boolean onTwoFactorPage() {
    UI ui = UI.getCurrent();
    if (ui == null) {
      return false;
    }
    // Ask the router what is on screen rather than formatting the route to a URL and comparing
    // strings: the target chain is the same fact the router used to render the page. The location
    // check is the fallback for the moment during navigation when the chain is not populated yet —
    // the route value is read from the annotation, so it cannot drift from the page it names.
    if (ui.getInternals().getActiveRouterTargetsChain().stream()
        .anyMatch(TwoFactorAuthenticationMain.class::isInstance)) {
      return true;
    }
    var location = ui.getInternals().getActiveViewLocation();
    String route = TwoFactorAuthenticationMain.class.getAnnotation(Route.class).value();
    return location != null && route.equals(location.getPath());
  }
}
