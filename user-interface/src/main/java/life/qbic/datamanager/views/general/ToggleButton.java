package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.checkbox.Checkbox;

/**
 * Custom Toggle Button adapted via css from the default {@link Checkbox} component
 */
public class ToggleButton extends Checkbox {

  public ToggleButton(String text) {
    addClassName("toggle-button");
    this.setLabel(text);
  }

}
