package life.qbic.datamanager.views;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.Runnables;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;

/**
 * <b>Small UI utility class</b>
 *
 * <p>Provides safe wrapper for {@link Runnables} that access the UI and are executed from outside
 * the UI thread.</p>
 *
 * @since 1.12.0
 */
public class UiUtil {

  /**
   * Always wrap any runnable task that manipulates the ui with this wrapper.
   * <p>
   * The wrapper contains safeguards like UI null-checks, attachment checks, session lock checks and
   * runs the task accordingly in a UI thread-safe manner.
   *
   * @param task the task to run
   * @since 1.12.0
   */
  public static void onUI(UI ui, Runnable task) {
    if (ui == null || isDetached(ui)) {
      return;
    }
    var session = ui.getSession();
    if (session == null) {
      return;
    }
    if (session.hasLock()) {
      task.run();  // we are already on the UI thread
    } else {
      try {
        ui.access(() -> {
          if (ui.isAttached()) {
            task.run();
          }
        });
      } catch (UIDetachedException ignore) { /* drop task if UI is gone */ }
    }
  }

  private static boolean isDetached(UI ui) {
    requireNonNull(ui);
    return !ui.isAttached();
  }

}
