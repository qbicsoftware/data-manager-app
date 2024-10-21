package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutEventListener;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.server.Command;
import java.util.Objects;

/**
 * A {@link Dialog} with additional functionality.
 * <p>
 *
 * @see Dialog
 * @since 1.4.0
 */
public class QbicDialog extends Dialog {

  private ShortcutRegistration escShortcut;

  public QbicDialog() {
    setCloseOnOutsideClick(false);
    setCloseOnEsc(false);
    setEscAction(it -> this.close());
  }

  public void setEscAction(ShortcutEventListener listener) {
    if (Objects.nonNull(escShortcut)) {
      escShortcut.remove();
    }
    escShortcut = Shortcuts.addShortcutListener(this, listener, Key.ESCAPE);
  }

  public void setEscAction(Command command) {
    if (Objects.nonNull(escShortcut)) {
      escShortcut.remove();
    }
    escShortcut = Shortcuts.addShortcutListener(this, command, Key.ESCAPE);
  }
}
