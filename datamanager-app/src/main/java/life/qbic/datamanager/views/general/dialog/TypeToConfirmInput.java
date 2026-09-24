package life.qbic.datamanager.views.general.dialog;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import org.jspecify.annotations.NonNull;

/**
 * <b>Type-to-confirm input</b>
 * <p>
 * A guarded confirmation input for a destructive action: the caller must retype the exact
 * resource name (e.g. a group name) before the destruction can be triggered. The guard is
 * intended to be presented <em>inline</em> next to the destructive action, not inside a modal
 * (GitHub/GitLab-style "Type {@code <name>} to confirm"), keeping the surrounding context
 * visible while the user deliberately commits to an irreversible operation.
 * <p>
 * Matching semantics follow the case-insensitive name semantics of the group domain
 * ({@code GROUP-NFR-02}): a value passes validation iff it matches the expected name exactly,
 * ignoring surrounding whitespace and case. Substrings or partial matches never pass.
 * <p>
 * The component is composed rather than a {@link TextField} subclass because Vaadin's {@code
 * TextField} already declares a no-argument {@code validate()} method returning {@code void};
 * implementing the {@link UserInput} validation contract therefore requires composition. The
 * underlying field is exposed via {@link #textField()} so callers can register a value-change
 * listener to gate a destructive button's enabled state reactively and can focus/clear the
 * field. Callers must still re-run {@link #validate()} server-side before executing the
 * destructive action (never trust the button state alone).
 *
 * @since 1.20.0
 */
public class TypeToConfirmInput extends Div implements UserInput {

  @Serial
  private static final long serialVersionUID = -2012222222222222227L;

  private final String expectedName;
  private final TextField textField;

  /**
   * Creates a type-to-confirm field for the given expected name.
   *
   * @param expectedName the exact name the caller must type; must not be {@code null}
   */
  public TypeToConfirmInput(String expectedName) {
    this.expectedName = requireNonNull(expectedName, "expectedName must not be null");
    this.textField = new TextField("Type \"" + expectedName + "\" to confirm");
    textField.setPlaceholder(expectedName);
    textField.setValueChangeMode(ValueChangeMode.EAGER);
    // Focus the field as soon as it appears so the committed user only has to type.
    textField.setAutofocus(true);
    textField.addClassName("type-to-confirm-input__field");
    addClassName("type-to-confirm-input");
    add(textField);
  }

  /**
   * Whether the current value equals the expected name (trimmed, case-insensitive, exact).
   *
   * @return {@link InputValidation#passed()} on exact match, {@link InputValidation#failed()}
   * otherwise
   */
  @Override
  public @NonNull InputValidation validate() {
    return textField.getValue().trim().equalsIgnoreCase(expectedName)
        ? InputValidation.passed() : InputValidation.failed();
  }

  /**
   * Whether the caller has started typing at all.
   *
   * @return {@code true} if the field holds any non-whitespace input, else {@code false}
   */
  @Override
  public boolean hasChanges() {
    return !textField.getValue().trim().isEmpty();
  }

  /**
   * The underlying text field. Callers use it to register a value-change listener (for button
   * gating) and to focus/clear the input.
   *
   * @return the wrapped text field; never {@code null}
   */
  public TextField textField() {
    return textField;
  }
}