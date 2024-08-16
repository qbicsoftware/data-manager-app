package life.qbic.datamanager.views.notifications;

/**
 * Can close without triggering any close listeners.
 * <p>
 * Intended to be used together with {@link WithCloseListener}.
 */
public interface CloseableWithoutListeners {

  /**
   * Close this without triggering
   * {@link life.qbic.datamanager.views.notifications.WithCloseListener.DialogCloseListener}s.
   */
  void closeIgnoringListeners();

}
