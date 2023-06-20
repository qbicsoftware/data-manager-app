package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
/**
 * <b>Dialog to create something</b>
 *
 * <p>Can be used to display the necessary inputs to create projects etc.
 * Every DialogWindow has two buttons: One to conform, one to close the DialogWindow without making
 * changes. Button titles are set in the respective Class using DialogWindow.</p>
 *
 * @since 1.0.0
 */
public class DialogWindow extends Dialog {

  protected final Button confirmButton = new Button();
  protected final Button cancelButton = new Button();

  protected DialogWindow(String confirmLabel, String cancelLabel) {
    this.addClassName("dialog");
    confirmButton.setText(confirmLabel);
    confirmButton.addClassName("confirm-button");
    cancelButton.setText(cancelLabel);
  }

  protected static class Container<T> {

    private T value;

    public T value() {
      return this.value;
    }

    public void setValue(T newValue) {
      this.value = newValue;
    }

  }
}
