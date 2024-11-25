package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/**
 * <b>Dialog Header</b>
 *
 * <p>A pre-formatted dialog header that shall be used as header for dialogs.</p>
 *
 * @since 1.7.0
 */
public class DialogHeader extends Div {

  private final SimpleDialog dialog;

  private DialogHeader() {
    dialog = null;
  }

  private DialogHeader(SimpleDialog dialog, String title) {
    this.dialog = Objects.requireNonNull(dialog);
    setText(title);
    addClassNames("heading-3", "dialog-header-text-color");
    dialog.setHeader(this);
  }

  public static DialogHeader with(SimpleDialog dialog, String title) {
    return new DialogHeader(dialog, title);
  }

}
