package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
/**
 * <b>Dialog to create something</b>
 *
 * <p>Can be used to display the necessary inputs to create projects etc.</p>
 *
 * @since 1.0.0
 */
public class DialogWindow extends Dialog {

  private final Button createButton = new Button("Create");

  protected DialogWindow() {
    this.addClassName("dialog");
    createButton.addClassName("create-button");
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
