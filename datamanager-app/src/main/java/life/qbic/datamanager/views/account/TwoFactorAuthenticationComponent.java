package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
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
 * <p>There is deliberately no "policy" hint here yet. The mode (off/optional/required) is a
 * deployment setting that lands with the login challenge in M4, and a warning about an enforcement
 * date that nothing enforces would be a promise the product cannot keep.</p>
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

  private final Span factorSummary = new Span();
  private final Div factorList = new Div();
  private final Span recoverySummary = new Span();
  private final Span policyNote = new Span();
  private final Button enrolButton = new Button("Set up authenticator app");
  private final Button generateRecoveryCodesButton = new Button("Generate recovery codes");

  public TwoFactorAuthenticationComponent() {
    addClassName("two-factor-authentication-component");

    SettingsSection factorSection = new SettingsSection("Two-factor authentication",
        "A second step at login, so a stolen password is not enough on its own. "
            + "An authenticator app on your phone is all you need.");
    enrolButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    factorSection.addAction(enrolButton);
    factorSummary.addClassName("mfa-summary");
    policyNote.addClassName("mfa-policy-note");
    factorList.addClassName("mfa-factor-list");
    factorSection.addContent(factorSummary, policyNote, factorList);

    SettingsSection recoverySection = new SettingsSection("Recovery codes",
        "Single-use codes to get back in if you lose your phone. They are shown once and "
            + "cannot be displayed again.");
    recoverySection.addAction(generateRecoveryCodesButton);
    recoverySummary.addClassName("mfa-summary");
    recoverySection.addContent(recoverySummary);

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
    factorList.removeAll();
    if (factors.isEmpty()) {
      factorSummary.setText("No authenticator app is set up for this account yet.");
      enrolButton.setText("Set up authenticator app");
      return;
    }
    factorSummary.setText(factors.size() == 1
        ? "One authenticator app is set up. It is required the next time you sign in."
        : "%d authenticator apps are set up. Any of them can be used to sign in."
            .formatted(factors.size()));
    enrolButton.setText("Set up another authenticator app");
    factors.forEach(factor -> factorList.add(buildFactorCard(factor)));
  }

  /**
   * States whether the deployment currently asks for a factor at sign-in.
   *
   * <p>Without this a user who enrolled has no way to know whether the step is simply not enforced
   * yet — and "I set it up and nothing happened" is the most common question the feature can produce.
   * It is also the quickest way for an operator to confirm which posture the running application
   * actually read, without opening a log.</p>
   *
   * @param mode the posture the application started with
   */
  public void setPolicyMode(MfaPolicyMode mode) {
    switch (mode) {
      case OFF -> policyNote.setText(
          "Two-factor authentication is currently not required at sign-in for this installation.");
      case OPTIONAL -> policyNote.setText(
          "You will be asked for a code at sign-in, because this account has set up a factor.");
      case REQUIRED -> policyNote.setText(
          "Two-factor authentication is required at sign-in for every account.");
    }
  }

  /**
   * Renders how many recovery codes are still usable.
   *
   * @param count remaining unused codes
   */
  public void setRecoveryCodeCount(long count) {
    if (count == 0) {
      recoverySummary.setText("No unused recovery codes. Generate a set and keep it somewhere safe.");
      generateRecoveryCodesButton.setText("Generate recovery codes");
      return;
    }
    recoverySummary.setText((count == 1
        ? "1 unused recovery code left."
        : "%d unused recovery codes left.".formatted(count))
        + " Generating a new set invalidates the old one.");
    generateRecoveryCodesButton.setText("Generate a new set");
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
    remove.addClickListener(
        event -> fireEvent(new RemoveFactorRequestedEvent(this, event.isFromClient(),
            factor.id())));
    header.add(name, remove);

    Span meta = new Span(typeLabel(factor.type()) + " · added " + DATE.format(factor.createdAt())
        + " · " + lastUsed(factor));
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
