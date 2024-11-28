package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import java.util.Objects;

/**
 * <b>Dialog Body</b>
 *
 * <p>A pre-formatted dialog body that shall be used as header for body.</p>
 *
 * @since 1.7.0
 */
public class DialogBody {

  private final Component component;

  private DialogBody() {
    this.component = null;
  }

  private DialogBody(AppDialog dialog, Component component, UserInput userInput) {
    this.component = Objects.requireNonNull(component);
    dialog.setBody(component);
    dialog.registerUserInput(Objects.requireNonNull(userInput));
  }

  public DialogBody(AppDialog dialog, Component component) {
    this.component = Objects.requireNonNull(component);
    dialog.setBody(component);
  }

  public static DialogBody with(AppDialog simpleDialog, Component component, UserInput userInput) {
    return new DialogBody(simpleDialog, component, userInput);
  }

  public static DialogBody withoutUserInput(AppDialog simpleDialog, Component component) {
    return new DialogBody(simpleDialog, component);
  }

}
