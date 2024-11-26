package life.qbic.datamanager.views.strategy.dialog;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import java.util.Objects;
import life.qbic.datamanager.views.general.DialogWindow;

/**
 * <b>Closing With Warning Strategy</b>
 * <p>
 * A {@link DialogClosingStrategy} implementation, that will open a {@link ConfirmDialog} when
 * executed.
 *
 * @since 1.6.0
 */
public class ClosingWithWarningStrategy implements DialogClosingStrategy {

  private final ConfirmDialog confirmDialog;

  public ClosingWithWarningStrategy(DialogWindow window, ConfirmDialog warningDialog) {
    Objects.requireNonNull(window);
    this.confirmDialog = Objects.requireNonNull(warningDialog);
    confirmDialog.addConfirmListener(listener -> window.close());
    confirmDialog.addCancelListener(listener -> confirmDialog.close());
  }

  @Override
  public void execute() {
    confirmDialog.open();
  }
}
