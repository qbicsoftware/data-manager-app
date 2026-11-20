package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import java.io.Serial;
import java.util.Objects;

/**
 * <b>A button that puts a piece of text on the clipboard, as often as the user likes</b>
 *
 * <p>Difference to {@link CopyToClipBoardComponent}: that component is a small icon that switches to a
 * check mark, which suits an inline value (a setup key, a command, a token). This one is a real
 * {@link Button} with a label, so it has a pointer cursor, a focus ring and keyboard activation
 * without extra work — the properties a user needs when the copy is an <em>action</em> rather than a
 * decoration next to a value.</p>
 *
 * <p>It deliberately has <b>no state</b>. An earlier version of the recovery-code sheet used the
 * icon-switching component there, and on a deployment the action became visibly "used up" after the
 * first click: the check mark never switched back, so a second copy looked impossible. Reporting the
 * result in a separate live region — which the caller owns — keeps the button permanently enabled and
 * permanently honest, and it is also the accessible way round: {@code role="status"} on a node that
 * changes is announced, a swapped icon is not.</p>
 *
 * <p>The clipboard function itself is the one already shipped for the rest of the application
 * ({@code frontend/javascript/copytoclipboard.js}, imported by {@link CopyToClipBoardComponent}), so
 * there is one clipboard implementation and all of its Safari/legacy fallbacks stay in one place. The
 * module has to be imported by a component that is actually attached to the UI — a class carrying
 * {@code @JsModule} but never attached (a dialog handle, for example) contributes nothing.</p>
 *
 * @since 1.0.0
 */
@JsModule("./javascript/copytoclipboard.js")
public class CopyToClipboardButton extends Button {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * @param label the visible text, e.g. {@code Copy all codes}
   * @param icon  the icon to show in front of the label, e.g. {@link com.vaadin.flow.component.icon.VaadinIcon#COPY}
   */
  public CopyToClipboardButton(String label, Component icon) {
    super(label, Objects.requireNonNull(icon, "icon must not be null"));
  }

  /**
   * Writes the given text to the client clipboard.
   *
   * <p>Fire-and-forget by design: the browser gives no reliable "clipboard is now what you think it
   * is" answer (the write is a promise, and a refused permission is indistinguishable to the user from
   * a successful one), so the caller reports its own success message rather than inventing a verdict
   * the client never sent.</p>
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
