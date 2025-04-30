package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.button.Button;

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

  public Button createConfirmButton(String label) {
    return createButton(label,
        new String[]{"button-text-primary", "button-color-primary", "button-size-dialog-medium"});
  }

  public Button createCancelButton(String label) {
    return createButton(label, new String[]{"button-text"});
  }

  public Button createNavigationButton(String label) {
    return createConfirmButton(label);
  }

}
