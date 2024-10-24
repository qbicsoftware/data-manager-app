package life.qbic.datamanager.views.general.section;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.List;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ActionBar extends Div {

  private List<Button> buttons = new ArrayList<>();

  public ActionBar() {
    addClassName("actionbar");
  }

  public ActionBar(Button... buttons) {
    this();
    add(buttons);
  }

  public void addButtons(Button... buttons) {
    for (Button b : buttons) {
      add(b);
    }
  }

  public void addButton(Button button) {
    buttons.add(button);
    add(button);
  }

  public void removeButton(Button button) {
    buttons.remove(button);
    remove(button);
  }
}
