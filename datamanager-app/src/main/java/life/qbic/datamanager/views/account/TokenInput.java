package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;

/**
 * Token input component for the "Add External Provider Token" dialog.
 *
 * <p>Wraps a {@link PasswordField} together with instructional text
 * (instance name, encryption notice, link to the provider's token
 * settings page). Implements {@link UserInput} so the surrounding
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
   */
  TokenInput(String instanceDisplayName, String tokenCreationUrl) {
    addClassName("token-input");

    var description = new Paragraph(
        "Paste your personal access token from your "
            + instanceDisplayName + " account.");
    description.addClassNames("text-contrast-70pct", "text-size-s");

    passwordField = new PasswordField("Personal Access Token");
    passwordField.setPlaceholder("Paste token here");
    passwordField.setRequired(true);
    passwordField.setWidthFull();

    var helpLine = new Div();
    helpLine.addClassNames("text-size-xs", "text-contrast-60pct");
    helpLine.add(new Span(
        "Your token is stored encrypted and used only to access "
            + "your own restricted datasets. You can create one at: "));
    if (tokenCreationUrl != null && !"#".equals(tokenCreationUrl)) {
      helpLine.add(new Anchor(tokenCreationUrl,
          instanceDisplayName + " token settings", AnchorTarget.BLANK));
    } else {
      helpLine.add(new Span(
          "your " + instanceDisplayName + " account settings."));
    }

    add(description, passwordField, helpLine);
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
