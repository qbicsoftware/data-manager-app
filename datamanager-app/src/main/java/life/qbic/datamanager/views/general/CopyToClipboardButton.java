package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import java.io.Serial;
import java.util.Objects;

/**
 * <b>A button that puts a piece of text on the clipboard until the user says otherwise</b>
 *
 * <p>Difference to {@link CopyToClipBoardComponent}: that component is an icon that sits next to an
 * inline value (a setup key, a command, a token). This one is an <em>action</em>: a real
 * {@code <button>} with a label, so it has a pointer cursor, a focus ring and keyboard activation for
 * free, and it can be clicked as often as the user likes.</p>
 *
 * <h3>Why the confirmation is client-side</h3>
 * <p>The copy icon turns into a check mark for a moment and then returns to the copy icon. That state
 * lives in a {@code data-copied} attribute, set by {@code frontend/javascript/copyfeedback.js} and
 * styled by {@code components/copy-action.css}.</p>
 *
 * <p>It used to be a server-side state, restored through {@code CompletableFuture} +
 * {@code UI.accessLater}. That is a trap: <b>without push, {@code accessLater} runs on the next
 * request</b>, so the check mark stayed on screen and the action looked used up after the first
 * click — reported exactly like that from a deployed build. A confirmation that must be undone is
 * not server state; it is a decoration, and decorations belong on the client.</p>
 *
 * <p>The script is delegated on the {@code document} rather than installed per element on attach, so
 * it cannot lose the attach race that a one-shot {@code executeJs} install loses (no shadow root
 * lookups, no retry loop, no silent no-op).</p>
 *
 * <p>The clipboard function itself is the one already shipped for the rest of the application
 * ({@code frontend/javascript/copytoclipboard.js}, also used by {@link CopyToClipBoardComponent}), so
 * the Safari and legacy-browser fallbacks stay in one place. The module has to be imported by a
 * component that is actually attached to the UI — a class carrying {@code @JsModule} but never
 * attached (a dialog handle, for example) contributes nothing.</p>
 *
 * @since 1.0.0
 */
@JsModule("./javascript/copytoclipboard.js")
@JsModule("./javascript/copyfeedback.js")
public class CopyToClipboardButton extends NativeButton {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * @param label the visible text, e.g. {@code Copy all codes}
   */
  public CopyToClipboardButton(String label) {
    Objects.requireNonNull(label, "label must not be null");

    Icon copyIcon = VaadinIcon.COPY.create();
    copyIcon.addClassName("copy-action__icon--copy");
    Icon copiedIcon = VaadinIcon.CHECK.create();
    copiedIcon.addClassName("copy-action__icon--copied");

    // Both icons are in the same slot, so swapping them does not move the label.
    add(copyIcon, copiedIcon, new Span(label));
    addClassName("copy-action");
    getElement().setAttribute("data-copy-feedback", "");
  }

  /**
   * Writes the given text to the client clipboard.
   *
   * <p>Fire-and-forget by design: the browser gives no reliable "the clipboard is now what you think
   * it is" answer — the write is a promise, and a refused permission is indistinguishable to the user
   * from a successful one — so the visible confirmation is this component's own, not a verdict the
   * clipboard never sent.</p>
   *
   * @param text the text to place on the clipboard
   */
  public void copy(String text) {
    Objects.requireNonNull(text, "text must not be null");
    UI ui = UI.getCurrent();
    if (ui == null) {
      throw new IllegalStateException(
          "copy() needs a UI; the button is not attached (or the call is not in a UI request)");
    }
    ui.getPage().executeJs("window.copyToClipboard($0)", text);
  }
}
