package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <b>Action Bar</b>
 *
 * <p>An actionbar offers the user the possibility to perform some action
 * that is related to its placed context in the application.</p>
 * <p>
 * Action items can be activated or deactivated. Inactive control elements are disabled and hidden
 * (default), active control elements are enabled and shown.
 *
 * <p></p>
 * <b>Relevant CSS</b>
 * <p>
 * The relevant CSS classes for this component are:
 *
 * <ul>
 *   <li><code>actionbar</code></li>
 * </ul>
 *
 * @since 1.6.0
 */
public class ActionBar extends Div {

  private transient ControlStrategy controlStrategy;

  private List<Button> buttons = new ArrayList<>();

  public ActionBar() {
    addClassName("actionbar");
    controlStrategy = new DisableStrategy();
  }

  public ActionBar(Button... buttons) {
    this();
    addButtons(buttons);
  }

  private static void applyStrategy(ControlStrategy strategy, Button... buttons) {
    Arrays.stream(buttons).forEach(strategy::execute);
  }

  private static void setEnabled(Button button, boolean enabled) {
    button.setEnabled(enabled);
  }

  public void addButtons(Button... buttons) {
    for (Button button : buttons) {
      addButton(button);
    }
  }

  public void addButton(Button button) {
    buttons.add(button);
    add(button);
    applyStrategy(controlStrategy, button);
  }

  public void removeButton(Button button) {
    buttons.remove(button);
    remove(button);
  }

  public void deactivateAllControls() {
    changeStrategy(new DisableStrategy());
  }

  private void changeStrategy(ControlStrategy strategy) {
    this.controlStrategy = strategy;
    applyStrategy(controlStrategy, buttons.toArray(new Button[0]));
  }

  public void activateAllControls() {
    changeStrategy(new EnableStrategy());
  }

  private interface ControlStrategy {

    void execute(Button button);

  }

  private static class EnableStrategy implements ControlStrategy {

    @Override
    public void execute(Button button) {
      button.setEnabled(true);
      button.setVisible(true);
    }
  }

  private static class DisableStrategy implements ControlStrategy {

    @Override
    public void execute(Button button) {
      button.setEnabled(false);
      button.setVisible(false);
    }
  }
}
