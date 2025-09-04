package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import java.util.Objects;
import org.springframework.lang.NonNull;

/**
 * <b>Dialog Body</b>
 *
 * <p>A pre-formatted dialog body that shall be used as header for body.</p>
 *
 * @since 1.7.0
 */
public class DialogBody {

  private DialogBody(AppDialog dialog, Component component, UserInput userInput) {
    Objects.requireNonNull(component);
    dialog.setBody(component);
    dialog.registerUserInput(Objects.requireNonNull(userInput));
  }

  private DialogBody(AppDialog dialog, Component component) {
    Objects.requireNonNull(component);
    dialog.setBody(component);
  }

  public static DialogBody with(@NonNull AppDialog simpleDialog, @NonNull Component component,
      @NonNull UserInput userInput) {
    return new DialogBody(simpleDialog, component, userInput);
  }

  public static DialogBody withoutUserInput(AppDialog simpleDialog, Component component) {
    return new DialogBody(simpleDialog, component);
  }

}
