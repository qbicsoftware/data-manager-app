package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import java.util.Objects;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DialogBody {

  private final UserInput userInput;
  private final Component component;
  private final SimpleDialog dialog;

  private DialogBody() {
    this.dialog = null;
    this.userInput = null;
    this.component = null;
  }

  private DialogBody(SimpleDialog dialog, Component component, UserInput userInput) {
    this.dialog = Objects.requireNonNull(dialog);
    this.component = Objects.requireNonNull(component);
    this.userInput = Objects.requireNonNull(userInput);
    dialog.setBody(component);
    dialog.registerUserInput(userInput);
  }

  public static DialogBody with(SimpleDialog simpleDialog, Component component, UserInput userInput) {
    return new DialogBody(simpleDialog, component, userInput);
  }

}
