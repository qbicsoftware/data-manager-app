package life.qbic.datamanager.views.strategy.dialog;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import java.util.Objects;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ClosingWithWarningStrategy implements DialogClosingStrategy{

  private final DialogWindow window;

  private final ConfirmDialog confirmDialog;

  public ClosingWithWarningStrategy(DialogWindow window, ConfirmDialog warningDialog) {
    this.window = Objects.requireNonNull(window);
    this.confirmDialog = Objects.requireNonNull(warningDialog);
    confirmDialog.addConfirmListener(listener -> window.close());
    confirmDialog.addCancelListener(listener -> confirmDialog.close());
  }

  @Override
  public void execute() {
    confirmDialog.open();
  }
}
