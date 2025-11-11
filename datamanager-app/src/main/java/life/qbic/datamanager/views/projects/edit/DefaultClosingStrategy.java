package life.qbic.datamanager.views.projects.edit;

import java.util.Objects;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.strategy.dialog.DialogClosingStrategy;

/**
 * <b>Default Closing Strategy</b>
 *
 * <p>Provides a default simple strategy implementation, that just closes the
 * {@link DialogWindow}.</p>
 *
 * @since 1.6.0
 */
class DefaultClosingStrategy implements DialogClosingStrategy {

  private final DialogWindow window;

  private DefaultClosingStrategy(DialogWindow window) {
    this.window = Objects.requireNonNull(window);
  }

  protected static DialogClosingStrategy createDefaultStrategy(DialogWindow window) {
    return new DefaultClosingStrategy(window);
  }

  @Override
  public void execute() {
    window.close();
  }
}
