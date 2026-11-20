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
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.settings.SettingsSection;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.model.mfa.SecondFactor;
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
  private final Div factorList = new Div();
  private final Span recoverySummary = new Span();
  private final Div recoveryCard = new Div();
  private final Button enrolButton = new Button("Set up authenticator app");
  private final Button generateRecoveryCodesButton = new Button("Generate recovery codes");

  // The three inputs the sentences depend on, kept together: each setter stores its input and
  // re-renders, so a sentence can never be computed without the state it has to be true for.
  private int factorCount;
  private long unusedRecoveryCodes;
  private MfaPolicyMode mode = MfaPolicyMode.OFF;

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
    // long does it take?" — the two questions that decide whether people start at all.
    Span emptyStateTitle = new Span("Nothing set up yet.");
    emptyStateTitle.addClassName("mfa-empty-state__title");
    Span emptyStateHint = new Span(
        "You will need a phone with an authenticator app. Setting one up takes about a minute.");
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
        "Single-use codes to get back in if you lose your phone. They are shown once and cannot be "
            + "displayed again.");
    recoverySummary.addClassName("mfa-card__text");
    recoveryCard.addClassName("mfa-card");
    recoveryCard.addClassName("mfa-card--row");
    // Secondary, not tertiary: next to a solid primary the tertiary look read as *disabled* on a
    // deployed build. This reads as an enabled action that still ranks below the primary.
    generateRecoveryCodesButton.addClassName("mfa-secondary-action");
    recoveryCard.add(recoverySummary, generateRecoveryCodesButton);
    recoverySection.addContent(recoveryCard);

    add(factorSection, recoverySection);

    enrolButton.addClickListener(
        event -> fireEvent(new EnrolRequestedEvent(this, event.isFromClient())));
    generateRecoveryCodesButton.addClickListener(
        event -> fireEvent(new GenerateRecoveryCodesRequestedEvent(this, event.isFromClient())));

    setFactors(List.of());
    setRecoveryCodeCount(0);
  }

  /**
   * Renders the account's active factors.
   *
   * @param factors the active factors, newest first; an empty list is the not-set-up state
   */
  public void setFactors(List<SecondFactor> factors) {
    factorCount = factors.size();
    factorList.removeAll();
    factors.forEach(factor -> factorList.add(buildFactorCard(factor)));
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
  private void render() {
    boolean hasFactor = factorCount > 0;

    statusState.setText(TwoFactorSettingsCopy.factorSummary(factorCount));
    policyNote.setText(TwoFactorSettingsCopy.policyNote(mode, hasFactor));
    status.removeClassNames("mfa-status--enrolled", "mfa-status--empty");
    status.addClassName(hasFactor ? "mfa-status--enrolled" : "mfa-status--empty");

    emptyState.setVisible(!hasFactor);
    factorList.setVisible(hasFactor);

    enrolButton.setText(hasFactor ? "Add another authenticator app" : "Set up authenticator app");
    if (hasFactor) {
      enrolButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
    } else {
      enrolButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    }

    recoverySummary.setText(TwoFactorSettingsCopy.recoverySummary(unusedRecoveryCodes, hasFactor));
    generateRecoveryCodesButton.setText(
        TwoFactorSettingsCopy.recoveryActionLabel(unusedRecoveryCodes, hasFactor));
    generateRecoveryCodesButton.setEnabled(hasFactor);
    recoveryCard.removeClassName("mfa-card--muted");
    if (!hasFactor) {
      recoveryCard.addClassName("mfa-card--muted");
    }
  }

  private Div buildFactorCard(SecondFactor factor) {
    Div card = new Div();
    card.addClassName("mfa-card");

    Div header = new Div();
    header.addClassName("mfa-card__header");
    Span name = new Span(factor.displayName());
    name.addClassName("mfa-card__name");
    Button remove = new Button("Remove");
    remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
    // With two factors on one page, "Remove" alone is not an accessible name: a screen-reader user
    // cannot tell which factor a button belongs to before activating it.
    remove.setAriaLabel("Remove " + factor.displayName());
    remove.addClickListener(
        event -> fireEvent(new RemoveFactorRequestedEvent(this, event.isFromClient(),
            factor.id())));
    header.add(name, remove);

    // The card title already names the factor, and for an authenticator app that name *is* the type
    // label — printing both reads as "Authenticator app / Authenticator app · added …".
    String type = typeLabel(factor.type());
    String added = DATE.format(factor.createdAt());
    Span meta = new Span(type.equals(factor.displayName())
        ? "Added " + added + " · " + lastUsed(factor)
        : type + " · added " + added + " · " + lastUsed(factor));
    meta.addClassName("mfa-card__meta");

    card.add(header, meta);
    return card;
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
