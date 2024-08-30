package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;

/**
 * A {@link com.vaadin.flow.component.dialog.Dialog} with additional functionality.
 * <p>
 *
 * @see com.vaadin.flow.component.dialog.Dialog
 * @since 1.4.0
 */
public class QbicDialog extends com.vaadin.flow.component.dialog.Dialog {

  public QbicDialog() {
    setCloseOnOutsideClick(false);
    setCloseOnEsc(false);
    Shortcuts.addShortcutListener(this, this::close,
        Key.ESCAPE); //overwrite to point to close method instead
  }
}
