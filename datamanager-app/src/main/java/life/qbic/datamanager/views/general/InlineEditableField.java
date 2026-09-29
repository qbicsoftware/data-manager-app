package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldBase;
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
 *       cancel. Blur-to-outside commits by default (see {@link #setCommitOnBlurToOutside}).</li>
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

  /**
   * Which input widget backs the editable value: a single-line {@link TextField} or a
   * multi-line {@link TextArea}.
   */
  public enum InputKind {
    TEXT,
    TEXTAREA
  }

  private final Span label;
  private final TextFieldBase<?, String> editField;
  private final boolean textArea;
  private final Button editButton;
  private final Button saveButton;
  private final Button cancelButton;
  private final Div editControls;

  private boolean editable = true;
  private String currentValue;
  private boolean editing = false;
  private boolean commitOnBlurToOutside = true;
  private int minDisplayWidthCh = MIN_DISPLAY_WIDTH_CH;

  /** Minimum width (in ch) of the field in display mode, so short values don't collapse. */
  private static final int MIN_DISPLAY_WIDTH_CH = 12;

  /** Single-line text field. */
  public InlineEditableField(String label, String initialValue) {
    this(InputKind.TEXT, label, initialValue);
  }

  /**
   * Creates an inline editable field backed by the given input kind.
   *
   * @param kind         {@link InputKind#TEXT} for a single-line input, {@link InputKind#TEXTAREA}
   *                     for a multi-line input (e.g. long descriptions)
   * @param label        the row label
   * @param initialValue the current value
   */
  public InlineEditableField(InputKind kind, String label, String initialValue) {
    addClassName("inline-editable-field");

    this.currentValue = initialValue != null ? initialValue : "";

    this.label = new Span(label + ":");
    this.label.addClassName("inline-editable-field__label");

    this.textArea = kind == InputKind.TEXTAREA;
    this.editField = textArea ? new TextArea() : new TextField();
    if (kind == InputKind.TEXTAREA) {
      ((TextArea) this.editField).setMaxLength(500);
      ((TextArea) this.editField).setMinHeight("6em");
      addClassName("inline-editable-field--textarea");
    } else {
      ((TextField) this.editField).setMaxLength(80);
    }
    this.editField.setValue(this.currentValue);
    this.editField.setReadOnly(true);
    this.editField.addClassName("inline-editable-field__edit-field");
    // Clicking into the field enters edit mode, exactly like the edit button — both entry
    // points must behave identically. Blur is not used directly: a blur-triggered revert would
    // race the Save/Cancel clicks (blur fires before the click, hiding the controls under the
    // cursor). Instead a native focusout listener reports the actual focus target over RPC
    // (installBlurRpc), so an outside click commits and clicks on the field's own buttons keep
    // editing. Edit mode is also left explicitly via Save (Enter/check) or Cancel (Escape/close).
    this.editField.addFocusListener(e -> startEdit());
    // Enter/Escape handling: the host-level key listeners fire for the single-line field;
    // for the textarea the keys are typed inside its slotted shadow-DOM element, so those
    // events do not reach this listener (the browser newline is the default there). We do
    // not fight the framework: textarea edits are confirmed with the green save button.
    this.editField.addKeyDownListener(Key.ENTER, e -> triggerSave());
    this.editField.addKeyDownListener(Key.ESCAPE, e -> triggerCancel());
    // Blur handling: Vaadin's BlurEvent carries no relatedTarget, so we cannot tell server-side
    // whether focus moved to the Save/Cancel buttons or somewhere else. We attach a native
    // focusout listener that asks the browser which way focus went and reports it over RPC
    // (@ClientCallable). This is deterministic — no deferred executeJs().then() that depends on
    // a later UI flush. If focus moved inside the field or onto its own buttons (Save/Cancel
    // are DOM children of this component), keep editing; otherwise commit the draft.
    installBlurRpc();

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

  /**
   * Controls what happens when the user blurs the field while in edit mode:
   * <ul>
   *   <li>{@code true} (default): blur-to-outside commits the draft ({@code SaveEvent} fires,
   *   parent persists) — least surprise, no silent data loss.</li>
   *   <li>{@code false}: blur-to-outside reverts to the committed value (like Cancel).</li>
   * </ul>
   * This only applies when focus moves <em>outside</em> the component; clicking the field's
   * own Save/Cancel buttons never triggers a blur-commit (they keep editing until clicked).
   */
  public void setCommitOnBlurToOutside(boolean commitOnBlurToOutside) {
    this.commitOnBlurToOutside = commitOnBlurToOutside;
  }

  public boolean isCommitOnBlurToOutside() {
    return commitOnBlurToOutside;
  }

  /**
   * Raises the minimum display-mode width (in characters). Useful when a field's content is
   * short but the caller wants a more generous input (e.g. group names on a detail page).
   * Takes effect immediately: the display width is re-applied.
   */
  public void setMinDisplayWidth(int widthCh) {
    this.minDisplayWidthCh = Math.max(MIN_DISPLAY_WIDTH_CH, widthCh);
    if (!editing) {
      applyDisplayWidth();
    }
  }

  /**
   * Whether the current (pending) value differs from the last committed value.
   */
  public boolean hasChanges() {
    String raw = editField.getValue();
    String normalized = raw == null ? "" : raw.trim();
    String committed = currentValue == null ? "" : currentValue;
    return !normalized.equals(committed);
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
    // Textarea-backed fields: fluid width — CSS grows them to the available container width
    // on large screens and lets them shrink back to a comfortable minimum on small screens
    // (setMinDisplayWidth / caller-configurable), wrapping + scrolling vertically when the
    // content overflows (see settings.css). A fixed width would fight the responsive layout,
    // so we set only the lower bound here. Single-line fields still size to their content.
    if (textArea) {
      editField.getElement().getStyle()
          .setMinWidth(Math.max(MIN_DISPLAY_WIDTH_CH, minDisplayWidthCh) + "ch");
      editField.getElement().setAttribute("title", currentValue);
      return;
    }
    String placeholder = editField.getPlaceholder() != null ? editField.getPlaceholder() : "";
    int longest = Math.max(currentValue.length(), placeholder.length());
    int widthCh = Math.max(minDisplayWidthCh, (int) Math.round(longest * 1.2) + 3);
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


  /**
   * Package-private seam for the blur-commit decision so unit tests can exercise it without a
   * browser (the real path is {@link #attemptBlurCommit()}, invoked from the client-side
   * {@code receiveFocusout} handler when focus moves outside the component).
   */
  void attemptBlurCommitForTest() {
    attemptBlurCommit();
  }

  /**
   * True while the field is in edit mode (package-private, exposed for unit tests).
   */
  boolean isEditingForTest() {
    return editing;
  }

  /**
   * Directly enters a value for tests, bypassing the read-only display-mode toggle.
   */
  void setValueInternal(String value) {
    editField.setValue(value == null ? "" : value);
  }

  /**
   * Triggers the save path for tests.
   */
  void saveForTest() {
    triggerSave();
  }

  /**
   * The width (in {@code ch}) applied in display mode as a lower bound, for unit tests.
   * Delegates to {@link #applyDisplayWidth()} so a test can pin the width-stability contract
   * of textarea-backed fields (long content must not grow the Java-set width; the CSS grows
   * the field fluidly up to its cap instead).
   *
   * @return the applied min-width in {@code ch}, or {@code -1} if unset
   */
  int displayWidthForTest() {
    applyDisplayWidth();
    String minWidth = editField.getElement().getStyle().get("min-width");
    String width = editField.getElement().getStyle().get("width");
    String value = minWidth != null ? minWidth : width;
    if (value == null || !value.endsWith("ch")) {
      return -1;
    }
    return Integer.parseInt(value.replace("ch", ""));
  }

  /**
   * Blur-to-outside: the user left the field without pressing Save/Cancel. We keep the
   * changes (least surprise — no silent data loss), fire a {@link SaveEvent} so the parent
   * persists, and only leave edit mode if the parent did not reject the value by re-entering
   * edit mode with an error (e.g. validation failure).
   */
  private void attemptBlurCommit() {
    if (!editable || !editing) {
      return;
    }
    if (!commitOnBlurToOutside) {
      // cancel-on-blur semantics: discard the draft, leave edit mode (like Cancel).
      revertToDisplay();
      return;
    }
    String newValue = editField.getValue().trim();
    // If nothing changed, just leave edit mode quietly (no event, no persistence round-trip).
    if (!hasChanges()) {
      revertToDisplay();
      return;
    }
    // Fire SaveEvent and optimistically clear the editing chrome. If the parent validation
    // rejects the value (setError + startEditing), it re-enters edit mode and keeps the ring.
    revertToDisplay();
    fireEvent(new SaveEvent(this, true, newValue));
  }

  /**
   * Attaches a native {@code focusout} listener on the edit field (runs client-side on the blur
   * itself, so it is deterministic) and reports whether focus moved outside the component via
   * {@link #receiveFocusout(boolean)}. {@code setTimeout(..., 0)} ensures focus has settled
   * before {@code document.activeElement} is checked, so clicking Save/Cancel is never mistaken
   * for an outside click (the buttons are DOM children of this component).
   */
  private void installBlurRpc() {
    getElement().executeJs(
        "this.addEventListener(\"focusout\", e => {"
            + "  setTimeout(() => {"
            + "    const a = document.activeElement;"
            + "    const inside = a === this || this.contains(a);"
            + "    if (this.$server) this.$server.receiveFocusout(!inside);"
            + "  }, 0);"
            + "});");
  }

  /**
   * Called from the client when the edit field loses focus.
   *
   * @param focusOutside {@code true} if the newly focused element is outside this component,
   *                     {@code false} if focus moved to the field itself or its Save/Cancel
   */
  @ClientCallable
  private void receiveFocusout(boolean focusOutside) {
    if (focusOutside) {
      // commit (default) OR revert, per setCommitOnBlurToOutside
      attemptBlurCommit();
    }
    // focus inside → keep editing; nothing to do.
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
