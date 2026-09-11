package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;

/**
 * Inline Editable Field
 * <p>
 * A reusable label–value row that supports three states:
 * <ul>
 *   <li><b>Display mode</b> — shows the current value with an optional edit button.</li>
 *   <li><b>Edit mode</b> — entered by clicking into the field or via the edit button; both
 *       behave identically. Shows Save/Cancel buttons; Enter triggers save, Escape triggers
 *       cancel. Edit mode is only left explicitly, never on blur.</li>
 *   <li><b>Read-only mode</b> — shows the value without any edit affordance.
 *       Used when the user lacks permission to modify the field.</li>
 * </ul>
 * <p>
 * The component fires {@link SaveEvent} and {@link CancelEvent} so the parent handles
 * all business logic (service calls, error mapping). The parent can also show validation
 * errors inline via {@link #setError(String)} / {@link #clearError()}.
 */
public class InlineEditableField extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Span label;
  private final TextField editField;
  private final Button editButton;
  private final Button saveButton;
  private final Button cancelButton;
  private final Div editControls;

  private boolean editable = true;
  private String currentValue;
  private boolean editing = false;

  /** Minimum width (in ch) of the field in display mode, so short values don't collapse. */
  private static final int MIN_DISPLAY_WIDTH_CH = 12;

  public InlineEditableField(String label, String initialValue) {
    addClassName("inline-editable-field");

    this.currentValue = initialValue != null ? initialValue : "";

    this.label = new Span(label + ":");
    this.label.addClassName("inline-editable-field__label");

    this.editField = new TextField();
    this.editField.setValue(this.currentValue);
    this.editField.setReadOnly(true);
    this.editField.addClassName("inline-editable-field__edit-field");
    // Clicking into the field enters edit mode, exactly like the edit button — both entry
    // points must behave identically. There is deliberately NO blur listener: a blur-triggered
    // revert races with the Save/Cancel buttons (the blur fires before the click, hiding the
    // controls and swallowing the click) and causes jarring width/style changes when the user
    // tabs away. Edit mode is left explicitly via Save (Enter/check) or Cancel (Escape/close).
    this.editField.addFocusListener(e -> startEdit());
    this.editField.addKeyDownListener(Key.ENTER, e -> triggerSave());
    this.editField.addKeyDownListener(Key.ESCAPE, e -> triggerCancel());

    this.editButton = new Button(new Icon(VaadinIcon.EDIT));
    this.editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    this.editButton.addClassName("inline-editable-field__edit-btn");
    this.editButton.setAriaLabel("Edit " + label.toLowerCase());
    this.editButton.addClickListener(e -> startEdit());

    this.saveButton = new Button(new Icon(VaadinIcon.CHECK));
    this.saveButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SUCCESS);
    this.saveButton.setAriaLabel("Save " + label.toLowerCase());
    this.saveButton.addClickListener(e -> triggerSave());

    this.cancelButton = new Button(new Icon(VaadinIcon.CLOSE));
    this.cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
    this.cancelButton.setAriaLabel("Cancel editing " + label.toLowerCase());
    this.cancelButton.addClickListener(e -> triggerCancel());

    this.editControls = new Div(saveButton, cancelButton);
    this.editControls.addClassName("inline-editable-field__edit-controls");
    this.editControls.setVisible(false);

    Div valueArea = new Div();
    valueArea.addClassName("inline-editable-field__value-area");
    valueArea.add(editField, editButton, editControls);

    add(this.label, valueArea);

    applyDisplayWidth();
    applyEditableState();
  }

  // ── Configuration ─────────────────────────────────────────────

  /**
   * Controls whether the field can be edited. When {@code false}, no edit button is shown
   * and the field is permanently in display mode (read-only).
   */
  public void setEditable(boolean editable) {
    this.editable = editable;
    applyEditableState();
  }

  public boolean isEditable() {
    return editable;
  }

  // ── Value ─────────────────────────────────────────────────────

  public String getValue() {
    return currentValue;
  }

  /**
   * Updates the displayed value. If currently in edit mode, also updates the text field.
   */
  public void setValue(String value) {
    this.currentValue = value != null ? value : "";
    editField.setValue(this.currentValue);
    if (!editing) {
      applyDisplayWidth();
    }
  }

  // ── Error ─────────────────────────────────────────────────────

  /**
   * Shows an inline error message using Vaadin's native TextField error styling
   * (red border + error text below the field).
   */
  public void setError(String message) {
    editField.setInvalid(true);
    editField.setErrorMessage(message);
  }

  /**
   * Clears any previously shown error message.
   */
  public void clearError() {
    editField.setInvalid(false);
    editField.setErrorMessage(null);
  }

  // ── State transitions ─────────────────────────────────────────

  /**
   * Programmatically enters edit mode. No-op if not editable.
   */
  public void startEditing() {
    if (!editable) {
      return;
    }
    startEdit();
  }

  /**
   * Programmatically cancels edit mode. No-op if not in edit mode.
   */
  public void cancelEditing() {
    revertToDisplay();
  }

  // ── Events ────────────────────────────────────────────────────

  public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
    return addListener(SaveEvent.class, listener);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    return addListener(CancelEvent.class, listener);
  }

  // ── Internals ─────────────────────────────────────────────────

  private void applyEditableState() {
    editButton.setVisible(editable);
    editField.setReadOnly(!editable);
    if (!editable) {
      revertToDisplay();
    }
  }

  private void startEdit() {
    if (!editable || editing) {
      return;
    }
    editing = true;
    clearError();
    editField.setReadOnly(false);
    // Keep the content-sized width: resetting it here would make the field jump to a different
    // width on entering edit mode. The CSS min-width keeps short values comfortably editable.
    editField.getElement().removeAttribute("title");
    editField.addClassName("inline-editable-field__edit-field--active");
    editButton.setVisible(false);
    editControls.setVisible(true);
    editField.focus();
  }

  private void revertToDisplay() {
    if (!editing) {
      return;
    }
    editing = false;
    editField.setReadOnly(true);
    editField.removeClassName("inline-editable-field__edit-field--active");
    editField.setValue(currentValue);
    editControls.setVisible(false);
    editButton.setVisible(editable);
    applyDisplayWidth();
  }

  /**
   * Sizes the field to its content in display mode (Pattern B: content-sized display).
   * Long values like email addresses stay fully visible instead of being truncated by a
   * fixed-width input box. The native {@code title} attribute acts as a hover tooltip
   * safety net for extreme edge cases.
   */
  private void applyDisplayWidth() {
    String placeholder = editField.getPlaceholder() != null ? editField.getPlaceholder() : "";
    int longest = Math.max(currentValue.length(), placeholder.length());
    int widthCh = Math.max(MIN_DISPLAY_WIDTH_CH, (int) Math.round(longest * 1.2) + 3);
    editField.setWidth(widthCh + "ch");
    editField.getElement().setAttribute("title", currentValue);
  }

  private void triggerSave() {
    if (!editable || !editing) {
      return;
    }
    String newValue = editField.getValue().trim();
    fireEvent(new SaveEvent(this, true, newValue));
  }

  private void triggerCancel() {
    revertToDisplay();
    fireEvent(new CancelEvent(this, true));
  }

  // ── Events ───────────────────────────────────────────────────

  /**
   * Fired when the user confirms an edit (clicks Save or presses Enter).
   * Carries the trimmed new value for the parent to process.
   */
  public static class SaveEvent extends ComponentEvent<InlineEditableField> {

    @Serial
    private static final long serialVersionUID = 1L;
    private final String value;

    public SaveEvent(InlineEditableField source, boolean fromClient, String value) {
      super(source, fromClient);
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  /**
   * Fired when the user cancels an edit (clicks Cancel or presses Escape).
   */
  public static class CancelEvent extends ComponentEvent<InlineEditableField> {

    @Serial
    private static final long serialVersionUID = 1L;

    public CancelEvent(InlineEditableField source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
