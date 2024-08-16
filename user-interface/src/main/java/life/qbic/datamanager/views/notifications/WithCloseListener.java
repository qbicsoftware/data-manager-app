package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.dialog.Dialog;
import java.io.Serializable;
import life.qbic.datamanager.views.notifications.WithCloseListener.CloseEvent;

/**
 * <b>Listens for close events</b>
 * <p>
 * This interface can be used as replacement for
 * {@link com.vaadin.flow.component.dialog.Dialog.DialogCloseActionEvent}. In contrast to the
 * component listener based solution provided by vaadin, implementing classes are free to implement
 * and fire the close event any way they want.
 * <p>
 * The vaadin solution does not cover all instances of closing a dialog. If a dialog is closed based
 * on a call to {@link Dialog#close()}, then no
 * {@link com.vaadin.flow.component.dialog.Dialog.DialogCloseActionEvent} is fired. This interface
 * thus acts as an addition filling this gap if a dialog close event is fired in the close method.
 *
 * @since 1.4.0
 */
public interface WithCloseListener<C, T extends CloseEvent<C>> {

  /**
   * Add a listener that is informed when something is deemed to be closed.
   * <p>
   * In combination with the {@link CloseableWithoutListeners} interface and some custom logic in
   * implementing classes, the listener can be used to catch close events. The listener is informed
   * when the dialog is requested to close. Then you can decide whether to close or to keep opened
   * the dialog. It means that dialog won't close unless you call the
   * {@link CloseableWithoutListeners#closeIgnoringListeners()} method explicitly in the listener
   * implementation.
   *
   * @param listener the listener to add
   * @return the resulting
   * {@link life.qbic.datamanager.views.notifications.WithCloseListener.ListenerRegistration}
   */
  ListenerRegistration addCloseListener(DialogCloseListener<C, T> listener);

  /**
   * A listener for close events.
   *
   * @param <C> the component acting as source of the close event.
   * @param <T> the close event to listen to.
   * @since 1.4.0
   */
  @FunctionalInterface
  interface DialogCloseListener<C, T extends CloseEvent<C>> {

    /**
     * Action performed whenever the listener is triggered.
     *
     * @param closeEvent the event to consume.
     * @since 1.4.0
     */
    void onClose(T closeEvent);

  }

  /**
   * Adds functionality to remove an added listener.
   *
   * @since 1.4.0
   */
  @FunctionalInterface
  interface ListenerRemover {

    /**
     * An action that should remove the listener.
     *
     * @since 1.4.0
     */
    void removeListener();
  }

  /**
   * A listener registration. Contains the registered listener as well as a way to remove the
   * listener again.
   *
   * @param listener        the registered listener
   * @param listenerRemover an action removing the listener
   */
  record ListenerRegistration(DialogCloseListener<?, ?> listener,
                              ListenerRemover listenerRemover) implements
      Serializable {

  }

  /**
   * An event fired when some close action occurs.
   *
   * @param <C> the source component class
   * @since 1.4.0
   */
  interface CloseEvent<C> {

    C getSource();

    boolean isFromClient();
  }
}
