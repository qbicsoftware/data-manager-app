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

  private static final String TERTIARY_THEME_NAME = "tertiary";

  private static Button createButton(String label, String...
      classNames) {
    Button button = new Button(label);
    if (classNames != null && classNames.length > 0) {
      button.addClassNames(classNames);
    }
    return button;
  }

  public Button createTertirayButton(String label, Icon icon) {
    var button = createButton(label, "button-text padding-none");
    button.setThemeName(TERTIARY_THEME_NAME);
    button.setIcon(icon);
    return button;
  }

  public Button createIconButton(Icon icon) {
    var button = createButton(null, "button-text padding-none button-icon-only primary");
    button.setThemeName(TERTIARY_THEME_NAME);
    button.setIcon(icon);
    return button;
  }

  public Button createGreyIconButton(Icon icon) {
    var button = createButton(null, "button-text padding-none button-icon-only");
    button.setThemeName(TERTIARY_THEME_NAME);
    button.setIcon(icon);
    return button;
  }

  public Button createConfirmButton(String label) {
    return createButton(label, "button-text-primary", "button-color-primary");
  }

  public Button createDangerButton(String label) {
    return createButton(label, "button-text-primary", "button-danger");
  }

  public Button createCancelButton(String label) {
    return createButton(label, "button-text");
  }

  public Button createNavigationButton(String label) {
    return createConfirmButton(label);
  }

}
