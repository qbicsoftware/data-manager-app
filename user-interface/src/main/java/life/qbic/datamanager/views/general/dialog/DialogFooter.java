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
    if (abortText != null) {
      var cancelButton = buttonFactory.createCancelButton(abortText);
      add(cancelButton, confirmButton);
      cancelButton.addClickListener(e -> dialog.cancel());
    } else {
      add(confirmButton);
    }
    dialog.setFooter(this);
    confirmButton.addClickListener(e -> dialog.confirm());
  }

  private DialogFooter() {
    dialog = null;
  }

  public static DialogFooter with(AppDialog dialog, String abortText, String confirmText) {
    return new DialogFooter(dialog, abortText, confirmText);
  }

  /**
   * Creates a footer with only one button, a confirm button that also triggers the
   * {@link AppDialog#confirm()} action when clicked.
   * <p>
   * This footer can be used for dialogs that are for display purposes only and do not have any user
   * input fields.
   *
   * @param dialog      the dialog to bind to
   * @param confirmText the button text to display for the confirmation
   * @return A dialog footer bound to the provided dialog with only one button
   * @since 1.9.0
   */
  public static DialogFooter withConfirmOnly(AppDialog dialog, String confirmText) {
    return new DialogFooter(dialog, null, confirmText);
  }

  public AppDialog getDialog() {
    return dialog;
  }
}
