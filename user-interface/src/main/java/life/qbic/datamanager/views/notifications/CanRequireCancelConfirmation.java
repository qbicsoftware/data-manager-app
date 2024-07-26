package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.general.ConfirmDialog;
import life.qbic.datamanager.views.general.Dialog;

/**
 * Provides basic functionality for catching dialog closing and requesting a user confirmation.
 * <p>
 * Classes implementing this interface must extend either {@link ConfirmDialog} or {@link Dialog}.
 *
 * @since <version tag>
 */
public interface CanRequireCancelConfirmation {

  default void requireCancelConfirmation() {
    if (this instanceof ConfirmDialog confirmDialog) {
      confirmDialog.addDialogCloseListener(dialogClosed -> askForConfirmation());
      confirmDialog.addCancelListener(cancelEvent -> askForConfirmation());
      confirmDialog.addRejectListener(rejectEvent -> askForConfirmation());
    } else if (this instanceof Dialog dialog) {
      dialog.addDialogCloseActionListener(dialogClosed -> askForConfirmation());
    } else {
      throw new UnsupportedOperationException(
          "Class " + this.getClass().getName() + " must provide an implementation.");
    }
  }

  private void askForConfirmation() {
    ConfirmDialog cancelConfirmationDialog = getCancelConfirmedDialog();
    cancelConfirmationDialog.addCancelListener(
        cancelCanceledEvent -> cancelCanceledEvent.getSource().close());
    cancelConfirmationDialog.addConfirmListener(confirmEvent -> {
      confirmEvent.getSource().close();
      onCancelConfirmed();
    });
    cancelConfirmationDialog.open();
  }

  default void onCancelConfirmed() {
    if (this instanceof Dialog dialog) {
      dialog.closeIgnoringListeners();
    }
    if (this instanceof ConfirmDialog confirmDialog) {
      confirmDialog.closeIgnoringListeners();
    }
  }

  default ConfirmDialog getDefaultCancelConfirmationDialog() {
    NotificationDialog dialog = NotificationDialog.warningDialog()
        .withTitle("Discard Changes?")
        .withContent(new Span(
            "By aborting the editing process and closing the dialog, you will loose all information entered."));
    dialog.setCancelable(true);
    dialog.setCancelText("Continue Editing");
    Button redButton = new Button("Discard Changes");
    redButton.addClassName("danger");
    dialog.setConfirmButton(redButton);
    return dialog;
  }

  default ConfirmDialog getCancelConfirmedDialog() {
    return getDefaultCancelConfirmationDialog();
  }
}
