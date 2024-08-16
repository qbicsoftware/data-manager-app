package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.notifications.WithCloseListener.CloseEvent;

/**
 * <b>Supports close only after confirmation</b>
 * <p>
 * This interface adds functionality to ask for confirmation after a close event is fired.
 * Implementing classes can modify the behaviour by providing their own implementation of
 * <ul>
 *   <li>{@link #getCloseConfirmationDialog()} - change the confirm dialog shown to ask for confirmation.
 *   <li>{@link #ignoresCloseCheckIfUnmodified()} - if {@code true} only asks for user confirmation if {@link TracksModification#wasModified()} is true
 *   <li>{@link #requireCloseConfirmation()} - needs to be called to require the close confirmation
 *   <li>{@link #onCloseActionConfirmed()} - defines what needs to be done after user confirmation of the close action
 * </ul>
 *
 * @since 1.4.0
 */

public interface SupportsCloseConfirmation<C, T extends CloseEvent<C>> extends
    WithCloseListener<C, T>, CloseableWithoutListeners, TracksModification {

  /**
   * The dialog used to confirm close actions. It does not need
   * {@link ConfirmDialog#addConfirmListener(ComponentEventListener)}.
   *
   * @return a {@link ConfirmDialog} used to confirm closing.
   * @since 1.4.0
   */
  default ConfirmDialog getCloseConfirmationDialog() {
    return getDefaultCloseConfirmationDialog();
  }

  private ConfirmDialog getDefaultCloseConfirmationDialog() {
    NotificationDialog dialog = NotificationDialog.warningDialog().withTitle("Discard Changes?")
        .withContent(new Span(
            "By aborting the editing process and closing the dialog, you will loose all information entered."));
    dialog.setCancelable(true);
    dialog.setCancelText("Continue Editing");
    Button redButton = new Button("Discard Changes");
    redButton.addClassName("danger");
    dialog.setConfirmButton(redButton);

    return dialog;
  }

  /**
   * Should the user be asked to confirm closing even if {@link #wasModified()} is false?
   *
   * @return true if {@link #requireCloseConfirmation()} should not trigger if
   * {@link #wasModified()} is false; returns false if {@link #wasModified()} should be ignored for
   * user confirmation.
   * @since 1.4.0
   */
  default boolean ignoresCloseCheckIfUnmodified() {
    return false;
  }

  /**
   * Registers a close listener and asks for close confirmation.
   *
   * @return the
   * {@link life.qbic.datamanager.views.notifications.WithCloseListener.ListenerRegistration} of the
   * close listener that can be used to remove the listener again.
   * @since 1.4.0
   */
  default ListenerRegistration requireCloseConfirmation() {
    return this.<C, T>addCloseListener(closeEvent -> askForConfirmation());
  }


  /**
   * Asks for close confirmation using the {@link #getCloseConfirmationDialog()} dialog. If the user
   * confirmed, executes {@link #onCloseActionConfirmed()}
   *
   * @since 1.4.0
   */
  private void askForConfirmation() {

    if (ignoresCloseCheckIfUnmodified() && !wasModified()) {
      onCloseActionConfirmed();
      return;
    }

    ConfirmDialog cancelConfirmationDialog = getCloseConfirmationDialog();

    cancelConfirmationDialog.addConfirmListener(confirmEvent -> onCloseActionConfirmed());
    cancelConfirmationDialog.open();
  }

  /**
   * Executed upon user confirmation of close action. Should probably continue the close.
   *
   * @since 1.4.0
   */
  default void onCloseActionConfirmed() {
    closeIgnoringListeners();
  }
}
