package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;


public class Dialog extends com.vaadin.flow.component.dialog.Dialog {

  private final List<Registration> dialogCloseListeners = new ArrayList<>();

  /**
   * Add a listener that controls whether the dialog should be closed or not. The listener is
   * informed when the user wants to close the dialog by clicking outside the dialog, or by pressing
   * escape. Then you can decide whether to close or to keep opened the dialog. It means that dialog
   * won't be closed automatically unless you call {@link #closeIgnoringListeners()} method
   * explicitly in the listener implementation. NOTE: adding this listener changes behavior of the
   * dialog. Dialog is closed automatically in case there are no any close listeners. And the
   * {@link #closeIgnoringListeners()} method should be called explicitly to close the dialog in
   * case there are close listeners.
   *
   * @param listener the listener to add
   * @return the registration to remove the listener
   */
  @Override
  public Registration addDialogCloseActionListener(
      ComponentEventListener<DialogCloseActionEvent> listener) {
    Registration registration = super.addDialogCloseActionListener(listener);
    dialogCloseListeners.add(registration);
    return () -> dialogCloseListeners.remove(registration);
  }


  @Override
  public void close() {
    if (dialogCloseListeners.isEmpty()) {
      closeIgnoringListeners();
      return;
    }
    fireEvent(new DialogCloseActionEvent(this, false));
  }

  /**
   * Closes the dialog ignoring close listeners.
   *
   * @see com.vaadin.flow.component.dialog.Dialog#close()
   */
  public void closeIgnoringListeners() {
    super.close();
  }
}
