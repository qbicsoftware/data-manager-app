package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.button.Button;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ButtonFactory {

  public Button createConfirmButton(String label) {
    return createButton(label, new String[]{"button-text-primary", "button-color-primary", "button-size-medium-dialog"});
  }

  private static Button createButton(String label, String[] classNames) {
    Button button = new Button(label);
    button.addClassNames(classNames);
    return button;
  }

  public Button createCancelButton(String label) {
    return createButton(label, new String[]{"button-text"});
  }

  public Button createNavigationButton(String label) {
    return createConfirmButton(label);
  }

}
