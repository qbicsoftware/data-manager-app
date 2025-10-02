package life.qbic.datamanager.views;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <b>Small UI utility class</b>
 *
 * <p>Provides safe wrapper for {@link Runnable} that access the UI and are executed from outside
 * the UI thread.</p>
 *
 * @since 1.12.0
 */
public final class UiHandle {

  private final AtomicReference<WeakReference<UI>> uiReference = new AtomicReference<>(new WeakReference<>(null));

  public void bind(UI ui) {
    requireNonNull(ui);
    uiReference.set(new WeakReference<>(ui));
  }

  public void unbind() {
    uiReference.set(new WeakReference<>(null));
  }

  /**
   * Always wrap any runnable task that manipulates the ui with this wrapper.
   * <p>
   * The wrapper contains safeguards like UI null-checks, attachment checks, session lock checks and
   * runs the task accordingly in a UI thread-safe manner.
   *
   * The {@link Runnable#run()} method will only be invoked, if the UI is attached.
   *
   * @param task the task to run
   * @since 1.12.0
   */
  private void push(Runnable task, boolean pushAfter) {
    var ui = get();
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
            if (pushAfter) {
              safePush(ui);
            }
          }
        });
      } catch (UIDetachedException ignore) { /* drop task if UI is gone */ }
    }
  }

  private static void safePush(UI ui) {
    if (ui.getPushConfiguration().getPushMode().isEnabled()) {
      ui.push();
    }
  }

  public void onUi(Runnable task) {
    push(task, false);
  }

  public void onUiAndPush(Runnable task) {
    push(task, true);
  }

  private static boolean isDetached(UI ui) {
    requireNonNull(ui);
    return !ui.isAttached();
  }

  private UI get() {
    var ui =  uiReference.get();
    return ui != null ? ui.get() : null;
  }

}
