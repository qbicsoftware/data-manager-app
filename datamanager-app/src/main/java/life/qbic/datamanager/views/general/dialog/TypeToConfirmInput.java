package life.qbic.datamanager.views.general.dialog;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.util.function.Supplier;
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

  private final Supplier<String> expectedNameSupplier;
  private final TextField textField;

  /**
   * Creates a type-to-confirm field whose expected name is resolved lazily from the given
   * supplier at validation time.
   * <p>
   * The supplier indirection keeps the guard in sync with the live resource name: the caller
   * may rename the resource (e.g. an inline-renamed group) after this input was constructed,
   * and the guard must then compare against the <em>current</em> name, not a stale snapshot.
   *
   * @param expectedNameSupplier resolves the exact name the caller must type; must not be
   *                             {@code null}, and must never return {@code null}
   */
  public TypeToConfirmInput(Supplier<String> expectedNameSupplier) {
    this.expectedNameSupplier = requireNonNull(expectedNameSupplier,
        "expectedNameSupplier must not be null");
    this.textField = new TextField();
    textField.setValueChangeMode(ValueChangeMode.EAGER);
    // Focus the field as soon as it appears so the committed user only has to type.
    textField.setAutofocus(true);
    textField.addClassName("type-to-confirm-input__field");
    addClassName("type-to-confirm-input");
    add(textField);
    refresh();
  }

  /**
   * Re-renders the prompt (label and placeholder) from the <em>current</em> expected name.
   * <p>
   * The label ({@code Type "<name>" to confirm}) and the placeholder are derived from the
   * expected name at construction; call this after the guarded resource was renamed so the
   * danger-zone prompt never shows a stale name while the button already validates against
   * the live name.
   */
  public void refresh() {
    String name = trimmedExpectedName();
    textField.setLabel("Type \"" + name + "\" to confirm");
    textField.setPlaceholder(name);
  }

  /**
   * The name the user must currently type, trimmed of surrounding whitespace (consistent with
   * how group names are stored). Kept current on every call so a rename of the wrapped
   * resource stays in sync.
   *
   * @return the trimmed expected name; never {@code null}
   */
  private String trimmedExpectedName() {
    String raw = expectedNameSupplier.get();
    if (raw == null) {
      throw new IllegalStateException("expectedNameSupplier must never return null");
    }
    return raw.trim();
  }

  /**
   * Whether the current value equals the expected name (trimmed, case-insensitive, exact).
   *
   * @return {@link InputValidation#passed()} on exact match, {@link InputValidation#failed()}
   * otherwise
   */
  @Override
  public @NonNull InputValidation validate() {
    String value = textField.getValue().trim();
    if (value.isEmpty()) {
      return InputValidation.failed();
    }
    return value.equalsIgnoreCase(trimmedExpectedName())
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