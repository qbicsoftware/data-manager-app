package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.PasswordField;
import life.qbic.datamanager.views.general.InfoBox;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;

/**
 * Token input component for the "Add External Provider Token" dialog.
 *
 * <p>Wraps a {@link PasswordField} together with a prominent info callout
 * that guides the user to create a personal access token on the provider's
 * settings page, and an encryption notice below the field. Implements
 * {@link UserInput} so the surrounding
 * {@link life.qbic.datamanager.views.general.dialog.AppDialog} drives
 * validation automatically via {@link #validate()}.</p>
 *
 * <p>The token value is returned as a {@code char[]} via
 * {@link #getToken()}. The service is responsible for zeroing the
 * array after use (ADR-0002 D1).</p>
 *
 * <p>When the remote instance rejects the token, {@link #setError}
 * displays the rejection message on the password field without
 * clearing the user's input, so they can correct and retry.</p>
 *
 * @since 1.12.0
 */
class TokenInput extends Div implements UserInput {

  private final PasswordField passwordField;

  /**
   * @param instanceDisplayName human-readable instance name (e.g. "Zenodo")
   * @param tokenCreationUrl    URL to the instance's token-settings page,
   *                            or {@code null} when no URL can be derived
   *                            (the button is then omitted)
   */
  TokenInput(String instanceDisplayName, String tokenCreationUrl) {
    addClassName("token-input");

    // ── Prominent info callout with CTA button ──
    // Uses the shared InfoBox component so the visual language is
    // consistent with the rest of the application. The "Create token"
    // button opens the provider's token-settings page in a new tab,
    // making the action one-click and impossible to overlook.
    var callout = new Div();
    callout.addClassNames("token-input__callout");

    var infoBox = new InfoBox()
        .setInfoText("To connect " + instanceDisplayName
            + ", you need a personal access token from your account.");

    callout.add(infoBox);

    if (tokenCreationUrl != null) {
      var createTokenButton = new Button(
          "Create token on " + instanceDisplayName,
          VaadinIcon.EXTERNAL_LINK.create());
      createTokenButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
      createTokenButton.addClassName("token-input__create-token-btn");
      createTokenButton.getElement().setAttribute("theme", "tertiary small");
      createTokenButton.addClickListener(e -> {
        UI.getCurrent().getPage().open(tokenCreationUrl, "_blank");
      });
      callout.add(createTokenButton);
    }

    // ── Password field ──
    passwordField = new PasswordField("Personal Access Token");
    passwordField.setPlaceholder("Paste token here");
    passwordField.setRequired(true);
    passwordField.setWidthFull();

    // ── Encryption notice (below the field) ──
    var encryptionNote = new Div();
    encryptionNote.addClassNames("token-input__encryption-note");
    encryptionNote.add(VaadinIcon.LOCK.create(),
        new Span("Your token is stored encrypted and used only to access "
            + "your own restricted datasets."));

    add(callout, passwordField, encryptionNote);
  }

  /**
   * Validates that the field is non-blank. On success, any previous
   * error state is cleared so the field is ready for a retry. The
   * field value is <strong>not</strong> cleared here — the confirm
   * action extracts it via {@link #getToken()} before clearing.
   */
  @Override
  public InputValidation validate() {
    String value = passwordField.getValue();
    if (value == null || value.isBlank()) {
      passwordField.setErrorMessage("Token must not be empty.");
      passwordField.setInvalid(true);
      return InputValidation.failed();
    }
    passwordField.setInvalid(false);
    passwordField.setErrorMessage("");
    return InputValidation.passed();
  }

  @Override
  public boolean hasChanges() {
    String value = passwordField.getValue();
    return value != null && !value.isBlank();
  }

  /**
   * Returns the current token value as a {@code char[]}.
   * Returns an empty array when the field is blank.
   */
  char[] getToken() {
    String value = passwordField.getValue();
    if (value == null || value.isBlank()) {
      return new char[0];
    }
    return value.toCharArray();
  }

  /**
   * Displays a service-side rejection message on the password field.
   * The user's current input is preserved so they can correct and
   * retry without re-pasting.
   */
  void setError(String errorMessage) {
    passwordField.setErrorMessage(errorMessage);
    passwordField.setInvalid(true);
  }

  /** Resets the field to its initial empty state. */
  void clearField() {
    passwordField.clear();
    passwordField.setInvalid(false);
    passwordField.setErrorMessage("");
  }
}
