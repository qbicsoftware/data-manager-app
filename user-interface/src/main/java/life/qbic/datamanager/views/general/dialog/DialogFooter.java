package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/**
 * <b>Dialog Footer</b>
 *
 * <p>A pre-formatted dialog footer that shall be used as footer for dialogs.</p>
 *
 * @since 1.7.0
 */
public class DialogFooter extends Div {

  private final AppDialog dialog;

  private DialogFooter(AppDialog dialog, String abortText, String confirmText) {
    this.dialog = Objects.requireNonNull(dialog);
    addClassNames("flex-horizontal", "gap-04", "footer");
    var buttonFactory = new ButtonFactory();
    var confirmButton = buttonFactory.createConfirmButton(confirmText);
    var cancelButton = buttonFactory.createCancelButton(abortText);
    add(cancelButton, confirmButton);
    dialog.setFooter(this);
    confirmButton.addClickListener(e -> dialog.confirm());
    cancelButton.addClickListener(e -> dialog.cancel());
  }

  public static DialogFooter with(AppDialog dialog, String abortText, String confirmText) {
    return new DialogFooter(dialog, abortText, confirmText);
  }

  private DialogFooter() {
    dialog = null;
  }

  public AppDialog getDialog() {
    return dialog;
  }
}
