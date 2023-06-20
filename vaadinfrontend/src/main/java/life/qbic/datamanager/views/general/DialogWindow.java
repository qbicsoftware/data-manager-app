package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
/**
 * <b>Dialog to create something</b>
 *
 * <p>Can be used to display the necessary inputs to create projects etc.
 * Every DialogWindow has two buttons: One to confirm, one to cancel the DialogWindow without making
 * changes. Button titles can be set in the respective Class using DialogWindow.</p>
 *
 * @since 1.0.0
 */
public class DialogWindow extends Dialog {

  protected final Button confirmButton = new Button("Confirm");
  protected final Button cancelButton = new Button("Cancel");

  protected DialogWindow() {
    this.addClassName("dialog-window");
    confirmButton.addClassName("confirm-button");
  }

  /**
   * Sets the label of the button that confirms the finished Dialog Window
   * @param confirmLabel
   */
  public void setConfirmButtonLabel(String confirmLabel){
    confirmButton.setText(confirmLabel);
  }

  /**
   * Sets the label of the cancel that cancels the Dialog Window
   * @param cancelLabel
   */
  public void setCancelButtonLabel(String cancelLabel){
    cancelButton.setText(cancelLabel);
  }

  protected static class Container<T> {

    private T value;

    /**
     * Returns the value stored in this container
     * @return
     */
    public T value() {
      return this.value;
    }

    /**
     * Sets the value stored in this container
     * @return
     */
    public void setValue(T newValue) {
      this.value = newValue;
    }

  }
}
