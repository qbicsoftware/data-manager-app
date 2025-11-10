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

  /**
   * Creates a new {@link DialogBody} instance and registers the {@link UserInput} by calling the
   * {@link AppDialog#registerUserInput(UserInput)} without the client required to do so manually.
   *
   * @param simpleDialog the {@link AppDialog} the body shall be created for
   * @param component    the component to be rendered in the body part of the dialog
   * @param userInput    the {@link UserInput} for input validation
   * @return a {@link DialogBody} for a given {@link AppDialog}
   * @since 1.7.0
   */
  public static DialogBody with(@NonNull AppDialog simpleDialog, @NonNull Component component,
      @NonNull UserInput userInput) {
    return new DialogBody(simpleDialog, component, userInput);
  }

  /**
   * Creates a new {@link DialogBody} instance for a given {@link AppDialog}.
   * <p>
   * <i>NOTE: since there is no {@link UserInput} is provided, the client needs to register it
   * explicitly via {@link AppDialog#registerUserInput(UserInput)} if automatic input validation
   * is desired.</i>
   *
   * @param simpleDialog the {@link AppDialog} the body shall be created for
   * @param component    the component to be rendered in the body part of the dialog
   * @return a {@link DialogBody} for a given {@link AppDialog}
   * @since 1.7.0
   */
  public static DialogBody withoutUserInput(AppDialog simpleDialog, Component component) {
    return new DialogBody(simpleDialog, component);
  }

}
