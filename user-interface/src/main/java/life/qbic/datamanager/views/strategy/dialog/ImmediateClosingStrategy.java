package life.qbic.datamanager.views.strategy.dialog;

import java.util.Objects;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * <b>Dialog Closing Strategy</b>
 *
 * <p>The simplest strategy implementation: it just closes the referenced {@link DialogWindow} on
 * execution.</p>
 *
 * @since 1.6.0
 */
public class ImmediateClosingStrategy implements DialogClosingStrategy {

  private final DialogWindow window;

  public ImmediateClosingStrategy(DialogWindow window) {
    this.window = Objects.requireNonNull(window);
  }

  @Override
  public void execute() {
    window.close();
  }
}
