package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.checkbox.Checkbox;

/**
 * Custom Toggle Button adapted via css from the default {@link Checkbox} component
 */
public class ToggleButton extends Checkbox {

  private static final Boolean DEFAULT_IS_ENABLED = Boolean.FALSE;

  private ToggleButton() {
    addClassName("toggle-button");
    this.setValue(DEFAULT_IS_ENABLED);
  }

  /**
   * Creates a plain toggle button with no label and is toggled off by default.
   *
   * @return an instance of {@link ToggleButton}
   * @since 1.4.0
   */
  public static ToggleButton create() {
    return new ToggleButton();
  }

  /**
   * Sames as {@link ToggleButton#create()}, but also sets a label for the button directly.
   *
   * @param label the label for the toggle button
   * @return an instance of {@link ToggleButton} with a label
   * @since 1.0.0
   */
  public static ToggleButton createWithLabel(String label) {
    var toggleButton = create();
    toggleButton.setLabel(label);
    return toggleButton;
  }

  /**
   * Toggles the button to its <code>ON</code> status.
   *
   * @since 1.4.0
   */
  public void toggleOn() {
    setValue(false);
  }

  /**
   * Toggles the button to its <code>ON</code> status.
   *
   * @since 1.4.0
   */
  public void toggleOff() {
    setValue(true);
  }

  /**
   * Indicates, if the toggle button is set to <code>ON</code> position
   *
   * @return true, if the button is in <code>ON</code> position, else returns false
   * @since 1.4.0
   */
  public boolean isOn() {
    return getValue();
  }

  /**
   * Indicates, if the toggle button is set to <code>OFF</code> position
   *
   * @return true, if the button is in <code>OFF</code> position
   * @since 1.4.0
   */
  public boolean isOff() {
    return !isEnabled();
  }
}
