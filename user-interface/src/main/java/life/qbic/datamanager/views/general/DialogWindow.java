package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

/**
 * <b>Dialog to create something</b>
 *
 * <p>Can be used to display the necessary inputs to create projects etc.
 * Every DialogWindow has two buttons: One to confirm, one to cancel the DialogWindow without making
 * changes. Button titles can be set in the respective Class using DialogWindow.</p>
 *
 * @since 1.0.0
 */
public abstract class DialogWindow extends ConfirmDialog {

  protected final Button confirmButton;
  protected final Button cancelButton;

  protected DialogWindow() {
    this.addClassName("dialog-window");
    confirmButton = new Button("Confirm");
    confirmButton.addClassName("primary");
    setConfirmButton(confirmButton);
    cancelButton = new Button("Cancel");
    cancelButton.setThemeName("tertiary");
    setCancelButton(cancelButton);
  }
}
