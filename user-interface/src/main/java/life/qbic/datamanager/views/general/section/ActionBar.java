package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ActionBar extends Div {

  private ControlStrategy controlStrategy;

  private List<Button> buttons = new ArrayList<>();

  public ActionBar() {
    addClassName("actionbar");
    controlStrategy = new DisableStrategy();
  }

  public ActionBar(Button... buttons) {
    this();
    addButtons(buttons);
  }

  public void addButtons(Button... buttons) {
    for (Button button : buttons) {
      addButton(button);
    }
  }

  private static void applyStrategy(ControlStrategy strategy, Button... buttons) {
    Arrays.stream(buttons).forEach(strategy::execute);
  }

  private static void setEnabled(Button button, boolean enabled) {
    button.setEnabled(enabled);
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
    }
  }

  private static class DisableStrategy implements ControlStrategy {

    @Override
    public void execute(Button button) {
      button.setEnabled(false);
    }
  }
}
