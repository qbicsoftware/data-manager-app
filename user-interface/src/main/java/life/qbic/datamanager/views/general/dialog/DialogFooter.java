package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import life.qbic.datamanager.views.general.ButtonFactory;

/**
 * <b>Dialog Footer</b>
 *
 * <p>A pre-formatted dialog footer that shall be used as footer for dialogs.</p>
 *
 * @since 1.7.0
 */
public class DialogFooter extends Div {

  private final AppDialog dialog;
  private static final String[] FOOTER_CSS_CLASSES = new String[]{"flex-horizontal", "gap-04",
      "footer"};

  private DialogFooter(AppDialog dialog, Button confirmButton) {
    this.dialog = Objects.requireNonNull(dialog);
    addClassNames(FOOTER_CSS_CLASSES);
    dialog.setFooter(this);
    add(confirmButton);
    confirmButton.addClickListener(e -> dialog.confirm());
  }

  private DialogFooter(AppDialog dialog, Button cancelButton, Button confirmButton) {
    this.dialog = Objects.requireNonNull(dialog);
    addClassNames(FOOTER_CSS_CLASSES);
    dialog.setFooter(this);
    add(cancelButton, confirmButton);
    cancelButton.addClickListener(e -> dialog.cancel());
    confirmButton.addClickListener(e -> dialog.confirm());
  }


  private DialogFooter() {
    dialog = null;
  }

  public static DialogFooter with(AppDialog dialog, String abortText, String confirmText) {
    var confirmButton = new ButtonFactory().createConfirmButton(confirmText);
    var cancelButton = new ButtonFactory().createCancelButton(abortText);
    return new DialogFooter(dialog, cancelButton, confirmButton);
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
    return new DialogFooter(dialog, new ButtonFactory().createConfirmButton(confirmText));
  }

  /**
   * Creates a footer with a cancel and a confirm button. As the confirm action associated is deemed
   * dangerous, the confirm button is styled in a way that indicates a dangerous operation. The
   * confirm button triggers {@link AppDialog#confirm()}
   * <p>
   * This footer is intended for potentially destructive dangerous operations on dialog
   * confirmation.
   *
   * @param dialog      the dialog to bind to
   * @param cancelText  the button text to display for cancelling the operation
   * @param confirmText the button text to display for the confirmation
   * @return A dialog footer bound to the provided dialog with a highlighted confirm button
   */
  public static DialogFooter withDangerousConfirm(AppDialog dialog, String cancelText,
      String confirmText) {
    var buttonFactory = new ButtonFactory();
    return new DialogFooter(dialog, buttonFactory.createCancelButton(cancelText),
        buttonFactory.createDangerButton(confirmText));
  }

  public AppDialog getDialog() {
    return dialog;
  }
}
