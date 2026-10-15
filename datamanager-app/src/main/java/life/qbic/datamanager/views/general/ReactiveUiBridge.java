package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.UI;
import java.util.Objects;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;

/**
 * The single bridge between the reactive query services and the Vaadin UI thread.
 *
 * <p>A reactive query must not update components from its scheduler thread. This adapter subscribes
 * to the {@link Mono} and re-enters the UI thread via {@link UI#access(Runnable)} for both the value
 * and the error signal, so view code never hand-rolls blocking or threading.</p>
 *
 * <p>This is the "one bridge, at the edge" from the implementation strategy (decision D7). Grid
 * page loads, which Vaadin renders synchronously, are a separate, deliberately deferred case; this
 * adapter covers the asynchronous callback shape used by overview sections.</p>
 *
 * @since 1.19.0
 */
public final class ReactiveUiBridge {

  private ReactiveUiBridge() {
    // static utility
  }

  /**
   * Subscribes to {@code source} and delivers its outcome on the UI thread.
   *
   * @param ui       the current UI; the callbacks run inside {@link UI#access(Runnable)}
   * @param source   the reactive query to run
   * @param onValue  called on the UI thread with the emitted value
   * @param onError  called on the UI thread if the query fails
   * @param <T>      the emitted value type
   */
  public static <T> void onUi(UI ui, Mono<T> source, Consumer<T> onValue,
      Consumer<Throwable> onError) {
    Objects.requireNonNull(ui, "ui must not be null");
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(onValue, "onValue must not be null");
    Objects.requireNonNull(onError, "onError must not be null");
    source.subscribe(
        value -> ui.access(() -> onValue.accept(value)),
        error -> ui.access(() -> onError.accept(error)));
  }
}
