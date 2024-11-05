package life.qbic.datamanager.views.strategy;

import java.util.Objects;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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
