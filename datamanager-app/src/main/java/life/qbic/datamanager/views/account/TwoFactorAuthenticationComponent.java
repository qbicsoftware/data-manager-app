package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.model.mfa.RecoveryCodes;
import life.qbic.identity.domain.model.mfa.SecondFactor;
import life.qbic.identity.domain.model.mfa.SecondFactorRemoval;
import life.qbic.identity.domain.model.mfa.SecondFactorType;

/**
 * <b>Two-factor authentication settings section</b>
 *
 * <p>Shows what the account currently has and offers the two actions of M3: set up an authenticator
 * app, and generate a set of recovery codes. The component renders state and raises intent events; it
 * never calls a service, so the flows (and their transactions) stay in
 * {@link TwoFactorAuthenticationMain}.</p>
 *
 * <p>The page states what this account has and what this installation asks for; both sentences come
 * from {@link TwoFactorSettingsCopy}, which is a pure function of the mode, the factor state and the
 * unused code count. That split is deliberate: an earlier version computed each sentence from one
 * input only, and produced two statements that were false of the page they appeared on.</p>
 *
 * <p>Nothing here promises enforcement the deployment does not perform — mode {@code OFF} says
 * plainly that no code will be asked for, even after enrolment.</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class TwoFactorAuthenticationComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault());

  private final Span policyNote = new Span();
  private final Span statusState = new Span();
  private final Div status = new Div();
  private final Div emptyState = new Div();
  private final Span emptyStateTitle = new Span();
  private final Span emptyStateHint = new Span();
  private final Div factorList = new Div();
  private final Span recoverySummary = new Span();
  private final Div recoveryMeter = new Div();
  private final Div recoveryCard = new Div();
  private final Button enrolButton = new Button("Set up authenticator app");
  private final Button generateRecoveryCodesButton = new Button("Generate recovery codes");
  private final Div recoveryCodesLost = new Div();

  // The three inputs the sentences depend on, kept together: each setter stores its input and
  // re-renders, so a sentence can never be computed without the state it has to be true for.
  private int factorCount;
  private long unusedRecoveryCodes;
  private MfaPolicyMode mode = MfaPolicyMode.OFF;
  private Instant graceDeadline;
  /** Whether the last one-time sheet was closed without the acknowledgement (MFA-91). */
  private boolean recoveryCodesUnacknowledged;

  public TwoFactorAuthenticationComponent() {
    addClassName("two-factor-authentication-component");

    /*
     * Section 1 — the factor. Three parts, in the order a user reads them: what is true of this
     * account (one emphatic line), what this installation will ask at sign-in (quiet, because it is
     * a deployment fact and not an account fact), then the factor itself.
     *
     * The description is one sentence on purpose: at this column width the previous text wrapped to
     * "...on your phone is all / you need.", leaving a two-word orphan line and a three-line header
     * that the right-aligned action could not align with.
     */
    SettingsSection factorSection = new SettingsSection("Two-factor authentication",
        "A second step at login, so a stolen password is not enough on its own.");
    factorSection.addAction(enrolButton);

    Icon shield = VaadinIcon.SHIELD.create();
    shield.addClassName("mfa-status__icon");
    statusState.addClassName("mfa-status__state");
    policyNote.addClassName("mfa-policy-note");
    Div statusText = new Div(statusState, policyNote);
    statusText.addClassName("mfa-status__text");
    status.add(shield, statusText);
    status.addClassName("mfa-status");

    // The empty state is a card-shaped invitation rather than a grey sentence: it is the first thing
    // a new account sees on this page, and it has to carry the answer to "what do I need?" and "how
    // long does it take?" — the two questions that decide whether people start at all. Its text is
    // mode-dependent and set in render().
    emptyStateTitle.addClassName("mfa-empty-state__title");
    emptyStateHint.addClassName("mfa-empty-state__hint");
    emptyState.addClassName("mfa-empty-state");
    emptyState.add(emptyStateTitle, emptyStateHint);

    factorList.addClassName("mfa-factor-list");
    factorSection.addContent(status, emptyState, factorList);

    /*
     * Section 2 — recovery codes, rendered as a card like the factor, because it is the same kind of
     * thing: an item the account holds, with an action that acts on it. Previously it was bare prose
     * whose action floated in the section header, so the two halves of one page had two different
     * shapes and the count was the fifth line of the section.
     */
    SettingsSection recoverySection = new SettingsSection("Recovery codes",
        "Single-use codes to get back in if you lose access to your app. They are shown once and "
            + "cannot be displayed again.");
    recoverySummary.addClassName("mfa-card__text");
    // Ten slots, one per code in a full set. Counted rather than read: the point is to answer "how many
    // have I got left?" in one glance, which "7 unused recovery codes left" in a grey sentence does not
    // do — the number is buried mid-line and the reader has to parse it.
    recoveryMeter.addClassName("mfa-recovery-meter");
    recoveryMeter.getElement().setAttribute("role", "img");
    Div recoveryText = new Div(recoveryMeter, recoverySummary);
    recoveryText.addClassName("mfa-recovery-text");
    recoveryCard.addClassName("mfa-card");
    recoveryCard.addClassName("mfa-card--row");
    // Secondary, not tertiary: next to a solid primary the tertiary look read as *disabled* on a
    // deployed build. This reads as an enabled action that still ranks below the primary.
    generateRecoveryCodesButton.addClassName("mfa-secondary-action");
    recoveryCard.add(recoveryText, generateRecoveryCodesButton);

    // The lost-sheet notice sits directly above the recovery card, because the action it asks for is
    // the button on that card. Hidden until a sheet actually goes unacknowledged (MFA-91).
    recoveryCodesLost.addClassName("mfa-recovery-lost");
    recoveryCodesLost.getElement().setAttribute("role", "alert");
    recoveryCodesLost.setVisible(false);

    recoverySection.addContent(recoveryCodesLost, recoveryCard);

    add(factorSection, recoverySection);

    enrolButton.addClickListener(
        event -> fireEvent(new EnrolRequestedEvent(this, event.isFromClient())));
    generateRecoveryCodesButton.addClickListener(
        event -> fireEvent(new GenerateRecoveryCodesRequestedEvent(this, event.isFromClient())));

    setFactors(List.of(), Map.of());
    setRecoveryCodeCount(0);
  }

  /**
   * Renders the account's active factors together with what removing each of them would do.
   *
   * <p>The outcome is computed by the application layer ({@code SecondFactorService.removalOutcome}),
   * i.e. by the same decision that performs the removal, so the button state and the server behaviour
   * cannot drift apart. Without it the page would offer a "Remove" that the server then refuses — the
   * worst of both worlds, because the user believes they removed their factor.</p>
   *
   * @param factors  the active factors, newest first; an empty list is the not-set-up state
   * @param outcomes what removing each factor would do, by factor id
   */
  public void setFactors(List<SecondFactor> factors,
      Map<Long, SecondFactorRemoval.Outcome> outcomes) {
    requireNonNull(factors, "factors must not be null");
    requireNonNull(outcomes, "outcomes must not be null");
    factorCount = factors.size();
    factorList.removeAll();
    factors.forEach(factor -> factorList.add(buildFactorCard(factor,
        outcomes.getOrDefault(factor.id(), SecondFactorRemoval.Outcome.NOT_REMOVABLE))));
    render();
  }

  /**
   * States whether the deployment currently asks for a factor at sign-in.
   *
   * <p>Without this a user who enrolled has no way to know whether the step is simply not enforced
   * yet — and "I set it up and nothing happened" is the most common question the feature can produce.
   * It is also the quickest way for an operator to confirm which posture the running application
   * actually read, without opening a log.</p>
   *
   * <p>The sentence is computed from the mode <em>and</em> the factor state (see
   * {@link TwoFactorSettingsCopy#policyNote}): {@code OPTIONAL} asks for nothing when no factor is
   * enrolled, and {@code OFF} asks for nothing even when one is.</p>
   *
   * @param mode the posture the application started with
   */
  public void setPolicyMode(MfaPolicyMode mode) {
    this.mode = requireNonNull(mode, "mode must not be null");
    render();
  }

  /**
   * Renders how many recovery codes are still usable.
   *
   * @param count remaining unused codes
   */
  public void setRecoveryCodeCount(long count) {
    unusedRecoveryCodes = count;
    render();
  }

  /**
   * Reports that a recovery-code sheet was shown and never acknowledged.
   *
   * <p>The codes cannot be re-displayed — only their hashes are stored (AUTH-R-03) — so the page
   * cannot repair the loss. It can refuse to be silent about it, which is the whole of MFA-91: the QA
   * run found that a refresh discarded the sheet and said nothing at all.</p>
   *
   * @param lost {@code true} when a sheet was closed without "I have saved them"
   */
  public void setRecoveryCodesLost(boolean lost) {
    this.recoveryCodesUnacknowledged = lost;
    render();
  }

  /**
   * The instant after which this account must present a code, if it is on a grace window.
   *
   * <p>Set by the page from the stored grace row; {@code null} means either the account has a factor or
   * the deployment is not enforcing. Rendering the date rather than the phrase "the grace period" is
   * the difference between a sentence a user can act on and one they can only worry about.</p>
   *
   * @param graceDeadline when password-only sign-in stops working for this account, or {@code null}
   */
  public void setGraceDeadline(Instant graceDeadline) {
    this.graceDeadline = graceDeadline;
    render();
  }

  /**
   * Writes the sentences and the action states from the current inputs.
   *
   * <p>Recovery codes are generated after the factor, not before: without a factor there is no
   * challenge, so a generated set could never be redeemed. The action is therefore disabled and the
   * sentence says how to get there.</p>
   *
   * <p>The enrolment action is demoted once the account has a factor. With one app set up, "add
   * another" is the least likely thing a visitor needs, so it must not hold the page's only saturated
   * button — that made the one action a worried user looks for (new recovery codes) look disabled by
   * comparison.</p>
   */
  /**
   * The status classes are mutually exclusive, and that is the whole point of listing them here.
   *
   * <p>{@code mfa-status--deadline} was missing from this list, so once an unenrolled account under
   * {@code REQUIRED} had rendered the deadline treatment, the class stayed. Enrolling then added
   * {@code mfa-status--enrolled}, but the deadline rule sits <em>later</em> in the stylesheet and
   * carries the same specificity, so it kept winning: the shield stayed warning-coloured until a reload
   * rebuilt the element — the exact symptom reported from a deployed build.</p>
   *
   * <p>Removing all three before adding one makes the state a function of the current inputs rather than
   * of everything the element has previously been.</p>
   */
  private static final String[] STATUS_CLASSES = {
      "mfa-status--enrolled", "mfa-status--deadline", "mfa-status--empty"
  };

  private void render() {
    boolean hasFactor = factorCount > 0;

    statusState.setText(TwoFactorSettingsCopy.factorSummary(factorCount));
    policyNote.setText(policySentence(hasFactor));
    status.removeClassNames(STATUS_CLASSES);
    // An unenrolled account under REQUIRED is the one case where the state line has a deadline, so it
    // carries the warning treatment rather than the plain "nothing set up yet".
    boolean onDeadline = !hasFactor && mode == MfaPolicyMode.REQUIRED;
    status.addClassName(hasFactor ? "mfa-status--enrolled"
        : onDeadline ? "mfa-status--deadline" : "mfa-status--empty");

    emptyState.setVisible(!hasFactor);
    factorList.setVisible(hasFactor);
    emptyStateTitle.setText(TwoFactorSettingsCopy.emptyStateTitle(mode));
    emptyStateHint.setText(TwoFactorSettingsCopy.emptyStateHint(mode));

    enrolButton.setText(TwoFactorSettingsCopy.enrolActionLabel(hasFactor));
    if (hasFactor) {
      enrolButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      enrolButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

    recoverySummary.setText(TwoFactorSettingsCopy.recoverySummary(unusedRecoveryCodes, hasFactor));
    renderRecoveryMeter(hasFactor);
    // Only while there is a factor: without one, generation is disabled and the sentence would tell
    // the user to press a button that cannot work.
    recoveryCodesLost.setText(RecoveryCodesDialog.unacknowledgedCodesNotice());
    recoveryCodesLost.setVisible(recoveryCodesUnacknowledged && hasFactor);
    generateRecoveryCodesButton.setText(
        TwoFactorSettingsCopy.recoveryActionLabel(unusedRecoveryCodes, hasFactor));
    generateRecoveryCodesButton.setEnabled(hasFactor);
    recoveryCard.removeClassName("mfa-card--muted");
    if (!hasFactor) {
      recoveryCard.addClassName("mfa-card--muted");
    }
  }

  /**
   * Renders one slot per recovery code in a full set: filled while unused, hollow once spent.
   *
   * <p><b>Why a meter and not just the count.</b> The sentence states the number, but it states it in the
   * middle of a grey line — the reader has to find it and parse it. Ten slots answer "how many have I got
   * left?" before the sentence is read at all, and they make the set's finite size visible: "7 unused" is
   * abstract, seven of ten is not.</p>
   *
   * <p><b>A full set is fixed.</b> Generating a new set always issues {@code RecoveryCodes.SET_SIZE}
   * codes, so the denominator is a constant of the domain, not of this view. That also means the meter
   * cannot lie about history: codes spent before a regeneration are not shown as spent, because the old
   * set stopped working entirely — the count is of <em>this</em> set.</p>
   *
   * <p>The slots are decoration for the eye. A screen reader gets the same fact from the sentence beside
   * them, so the meter is one image with an accessible label rather than ten meaningless spans — ten
   * "item" announcements would be noise in front of the actual sentence.</p>
   *
   * @param hasFactor whether the account has an active factor; without one there is no set to show
   */
  private void renderRecoveryMeter(boolean hasFactor) {
    recoveryMeter.removeAll();
    if (!hasFactor) {
      // No factor, no challenge, therefore no redeemable set (see the action's disabled state).
      // Rendering ten hollow slots here would suggest codes exist and are all spent.
      recoveryMeter.setVisible(false);
      return;
    }
    recoveryMeter.setVisible(true);

    int total = RecoveryCodes.SET_SIZE;
    // Clamp: a stale read must not draw eleven slots or a negative one. The count comes from the
    // database and the set size from the domain, and this method is the place they have to agree.
    int unused = (int) Math.max(0, Math.min(total, unusedRecoveryCodes));

    for (int index = 0; index < total; index++) {
      Span slot = new Span();
      slot.addClassName("mfa-recovery-meter__slot");
      slot.addClassName(index < unused ? "mfa-recovery-meter__slot--unused"
          : "mfa-recovery-meter__slot--used");
      recoveryMeter.add(slot);
    }
    recoveryMeter.getElement().setAttribute("aria-label",
        "%d of %d recovery codes unused".formatted(unused, total));
  }

  /**
   * The posture sentence, including the exact deadline when this account is on a grace window.
   *
   * <p>The date only exists under {@code REQUIRED} without a factor, which is the one case where the
   * deployment is counting down on this account. Everywhere else the mode/factor sentence is the whole
   * answer, and {@link TwoFactorSettingsCopy#requiredDeadlineNote} is not reached.</p>
   *
   * @param hasFactor whether the account has an active factor
   * @return the sentence to show under the status line
   */
  private String policySentence(boolean hasFactor) {
    if (mode == MfaPolicyMode.REQUIRED && !hasFactor && graceDeadline != null) {
      return TwoFactorSettingsCopy.requiredDeadlineNote(DATE.format(graceDeadline));
    }
    // No stored window (MfaGate reads that as "grant one", never "deny") or a factor is present:
    // the mode/factor sentence is the whole answer.
    return TwoFactorSettingsCopy.policyNote(mode, hasFactor);
  }

  private Div buildFactorCard(SecondFactor factor, SecondFactorRemoval.Outcome outcome) {
    Div card = new Div();
    card.addClassName("mfa-card");

    Div header = new Div();
    header.addClassName("mfa-card__header");
    Span name = new Span(factor.displayName());
    name.addClassName("mfa-card__name");
    header.add(name, cardActions(factor, outcome));

    // The card title already names the factor, and for an authenticator app that name *is* the type
    // label — printing both reads as "Authenticator app / Authenticator app · added …".
    String type = typeLabel(factor.type());
    String added = DATE.format(factor.createdAt());
    Span meta = new Span(type.equals(factor.displayName())
        ? "Added " + added + " · " + lastUsed(factor)
        : type + " · added " + added + " · " + lastUsed(factor));
    meta.addClassName("mfa-card__meta");

    card.add(header, meta);
    if (outcome == SecondFactorRemoval.Outcome.LAST_FACTOR_PROTECTED) {
      Span blocked = new Span("Two-factor authentication is required on this installation, so at "
          + "least one authenticator app has to stay set up.");
      blocked.addClassName("mfa-card__note");
      card.add(blocked);
    }
    if (outcome == SecondFactorRemoval.Outcome.TURNS_OFF_TWO_FACTOR) {
      Span consequence = new Span("Removing your only authenticator app turns two-factor "
          + "authentication off for this account and deletes your recovery codes.");
      consequence.addClassName("mfa-card__note");
      card.add(consequence);
    }
    return card;
  }

  /**
   * The per-factor action, named for what it actually does.
   *
   * <p>A removal that ends enrolment is not called "Remove": it is the account switching two-factor
   * authentication off, and it takes the recovery codes with it. Under {@code REQUIRED} the same
   * removal is refused, so the button is disabled with the reason in the card rather than hidden —
   * a missing button leaves the user guessing why.</p>
   */
  /**
   * The per-factor actions: rename, then remove.
   *
   * <p>Rename is tertiary and neutral — it changes a label, not a credential — and it is the only way to
   * fix a name after enrolment. Without it, a typo made in the setup dialog would be permanent, which is
   * the kind of thing that makes people remove and re-enrol a factor they did not need to touch.</p>
   */
  private Div cardActions(SecondFactor factor, SecondFactorRemoval.Outcome outcome) {
    Button rename = new Button("Rename");
    rename.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    rename.setAriaLabel("Rename " + factor.displayName());
    rename.addClickListener(event -> fireEvent(
        new RenameFactorRequestedEvent(this, event.isFromClient(), factor.id())));

    Div actions = new Div(rename, removalControl(factor, outcome));
    actions.addClassName("mfa-card__actions");
    return actions;
  }

  private Button removalControl(SecondFactor factor, SecondFactorRemoval.Outcome outcome) {
    if (outcome == SecondFactorRemoval.Outcome.TURNS_OFF_TWO_FACTOR) {
      Button turnOff = new Button("Turn off two-factor authentication");
      turnOff.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
      turnOff.setAriaLabel("Turn off two-factor authentication for this account");
      turnOff.addClickListener(event -> fireEvent(
          new RemoveFactorRequestedEvent(this, event.isFromClient(), factor.id())));
      return turnOff;
    }

    Button remove = new Button("Remove");
    remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    // With two factors on one page, "Remove" alone is not an accessible name: a screen-reader user
    // cannot tell which factor a button belongs to before activating it.
    remove.setAriaLabel("Remove " + factor.displayName());
    boolean blocked = outcome != SecondFactorRemoval.Outcome.ALLOWED;
    remove.setEnabled(!blocked);
    if (blocked) {
      remove.setTooltipText("At least one authenticator app has to stay set up.");
    } else {
      remove.addClickListener(event -> fireEvent(
          new RemoveFactorRequestedEvent(this, event.isFromClient(), factor.id())));
    }
    return remove;
  }

  private static String typeLabel(SecondFactorType type) {
    return switch (type) {
      case TOTP -> "Authenticator app";
      case WEBAUTHN -> "Passkey or security key";
    };
  }

  private static String lastUsed(SecondFactor factor) {
    Instant lastUsedAt = factor.lastUsedAt();
    return lastUsedAt == null ? "not used yet" : "last used " + DATE.format(lastUsedAt);
  }

  public com.vaadin.flow.shared.Registration addEnrolListener(
      ComponentEventListener<EnrolRequestedEvent> listener) {
    return addListener(EnrolRequestedEvent.class, listener);
  }

  public com.vaadin.flow.shared.Registration addRemoveListener(
      ComponentEventListener<RemoveFactorRequestedEvent> listener) {
    return addListener(RemoveFactorRequestedEvent.class, listener);
  }

  public com.vaadin.flow.shared.Registration addGenerateRecoveryCodesListener(
      ComponentEventListener<GenerateRecoveryCodesRequestedEvent> listener) {
    return addListener(GenerateRecoveryCodesRequestedEvent.class, listener);
  }

  /** The user wants to start an authenticator-app enrolment. */
  public static class EnrolRequestedEvent extends ComponentEvent<TwoFactorAuthenticationComponent> {

    @Serial
    private static final long serialVersionUID = 1L;

    public EnrolRequestedEvent(TwoFactorAuthenticationComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /** The user wants to relabel one factor, identified by its id. */
  public static class RenameFactorRequestedEvent
      extends ComponentEvent<TwoFactorAuthenticationComponent> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final long factorId;

    public RenameFactorRequestedEvent(TwoFactorAuthenticationComponent source, boolean fromClient,
        long factorId) {
      super(source, fromClient);
      this.factorId = factorId;
    }

    public long factorId() {
      return factorId;
    }
  }

  /**
   * Registers a listener for rename requests.
   *
   * @param listener called with the factor the user wants to relabel
   * @return a registration that removes the listener again
   */
  public com.vaadin.flow.shared.Registration addRenameListener(
      ComponentEventListener<RenameFactorRequestedEvent> listener) {
    return addListener(RenameFactorRequestedEvent.class, listener);
  }

  /** The user wants to remove one factor, identified by its id. */
  public static class RemoveFactorRequestedEvent
      extends ComponentEvent<TwoFactorAuthenticationComponent> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final long factorId;

    public RemoveFactorRequestedEvent(TwoFactorAuthenticationComponent source, boolean fromClient,
        long factorId) {
      super(source, fromClient);
      this.factorId = factorId;
    }

    public long factorId() {
      return factorId;
    }
  }

  /** The user wants a new set of recovery codes. */
  public static class GenerateRecoveryCodesRequestedEvent
      extends ComponentEvent<TwoFactorAuthenticationComponent> {

    @Serial
    private static final long serialVersionUID = 1L;

    public GenerateRecoveryCodesRequestedEvent(TwoFactorAuthenticationComponent source,
        boolean fromClient) {
      super(source, fromClient);
    }
  }
}
