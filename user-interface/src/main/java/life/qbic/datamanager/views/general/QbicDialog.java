package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.notifications.SupportsCloseConfirmation;
import life.qbic.datamanager.views.notifications.WithCloseListener.CloseEvent;

/**
 * A {@link com.vaadin.flow.component.dialog.Dialog} with additional functionality.
 * <p>
 * This class adds functionality to only close on user confirmation. For this simply call
 * {@link #requireCloseConfirmation()}.
 *
 * @see com.vaadin.flow.component.dialog.Dialog
 * @since 1.4.0
 */
public class QbicDialog extends com.vaadin.flow.component.dialog.Dialog implements
    SupportsCloseConfirmation<QbicDialog, CloseEvent<QbicDialog>> {

  private final List<ListenerRegistration> dialogCloseListeners = new ArrayList<>();
  private ListenerRegistration requireCloseConfirmationListener = null;
  private boolean ignoresModificationForCloseConfirmation = SupportsCloseConfirmation.super.ignoresCloseCheckIfUnmodified();
  private boolean modified = false;

  public QbicDialog() {
    setCloseOnOutsideClick(false);
    setCloseOnEsc(false);
    Shortcuts.addShortcutListener(this, this::close, Key.ESCAPE);
  }

  @Override
  public ListenerRegistration requireCloseConfirmation() {
    if (requireCloseConfirmationListener != null) {
      requireCloseConfirmationListener.listenerRemover().removeListener();
    }
    requireCloseConfirmationListener = SupportsCloseConfirmation.super.requireCloseConfirmation();
    return requireCloseConfirmationListener;
  }

  /**
   * Remove required close confirmation. Can be used to revert {@link #requireCloseConfirmation()}
   *
   * @since 1.4.0
   */
  public void allowCloseWithoutConfirmation() {
    if (Objects.nonNull(requireCloseConfirmationListener)) {
      requireCloseConfirmationListener.listenerRemover().removeListener();
    }
  }

  /**
   * Add a listener that controls whether the dialog closes or not. The listener is informed when
   * the dialog is requested to close. Then you can decide whether to close or to keep opened the
   * dialog. It means that dialog won't closed unless you call the {@link #closeIgnoringListeners()}
   * method explicitly in the listener implementation.
   * <p>
   * <i>NOTE:</i>
   * <b>Adding this listener changes behavior of the dialog.</b>
   * <p>
   * If there are no close listeners present, the {@link #close()} method closes the dialog.
   * Otherwise, the existing close listeners will prevent {@link #close()} from closing the dialog.
   * The listeners should call the {@link #closeIgnoringListeners()} method to close the dialog
   * instead.
   *
   * @param listener the listener to add
   * @return the resulting
   * {@link life.qbic.datamanager.views.notifications.WithCloseListener.ListenerRegistration}
   */
  @Override
  public ListenerRegistration addCloseListener(
      DialogCloseListener<QbicDialog, CloseEvent<QbicDialog>> listener) {
    com.vaadin.flow.shared.Registration registration = addListener(DialogCloseEvent.class,
        listener::onClose);

    ListenerRegistration listenerRegistration = new ListenerRegistration(listener,
        registration::remove);
    dialogCloseListeners.add(listenerRegistration);
    return listenerRegistration;
  }

  @Override
  public boolean ignoresCloseCheckIfUnmodified() {
    return this.ignoresModificationForCloseConfirmation;
  }

  /**
   * Sets whether unmodified dialogs require close confirmation.
   *
   * @param ignore should missing modification ignore the close check?
   * @param <S>    the class from which the method is called.
   * @return the modified dialog
   */
  public <S extends QbicDialog> S setIgnoreCloseCheckIfUnmodified(boolean ignore) {
    this.ignoresModificationForCloseConfirmation = ignore;
    return (S) this;
  }

  @Override
  public boolean wasModified() {
    return modified;
  }

  @Override
  public void setModified(boolean modified) {
    this.modified = modified;
  }

  /**
   * @see #close()
   */
  @Override
  public void closeIgnoringListeners() {
    super.close();
  }

  /**
   * Closes the dialog if no close listeners are present.
   * <p>
   * <p>
   * If there are close listeners, instead of closing the dialog, a {@link DialogCloseEvent} is
   * fired.
   */
  @Override
  public void close() {
    if (dialogCloseListeners.isEmpty()) {
      closeIgnoringListeners();
      return;
    }
    fireEvent(new DialogCloseEvent(this, false));
  }

  public static class DialogCloseEvent extends ComponentEvent<QbicDialog> implements
      CloseEvent<QbicDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public DialogCloseEvent(QbicDialog source, boolean fromClient) {
      super(source, fromClient);
    }

  }
}
