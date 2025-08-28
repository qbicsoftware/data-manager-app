package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;

/**
 * <b><Button Factory</b>
 * <p>
 * Centralises the styling for buttons in the dialog context.
 *
 * @since 1.7.0
 */
public class ButtonFactory {

  private static Button createButton(String label, String[] classNames) {
    Button button = new Button(label);
    button.addClassNames(classNames);
    return button;
  }

  public Button createTertirayButton(String label, Icon icon) {
    var button = createButton(label, new String[]{"button-text padding-none"});
    button.setThemeName("tertiary");
    button.setIcon(icon);
    return button;
  }

  public Button createConfirmButton(String label) {
    return createButton(label,
        new String[]{"button-text-primary", "button-color-primary"});
  }

  public Button createCancelButton(String label) {
    return createButton(label, new String[]{"button-text"});
  }

  public Button createNavigationButton(String label) {
    return createConfirmButton(label);
  }

}
