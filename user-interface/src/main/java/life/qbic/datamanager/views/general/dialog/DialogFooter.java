package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/**
 * <b>Dialog Footer</b>
 *
 * <p>A pre-formatted dialog footer that shall be used as footer for dialogs.</p>
 *
 * @since 1.7.0
 */
public class DialogFooter extends Div {

  private final AppDialog dialog;

  private DialogFooter(AppDialog dialog, String abortText, String confirmText) {
    this.dialog = Objects.requireNonNull(dialog);
    addClassNames("flex-horizontal", "gap-04");
    var confirmButton = createConfirmButton(confirmText);
    var cancelButton = createCancelButton(abortText);
    add(cancelButton, confirmButton);
    dialog.setFooter(this);
    confirmButton.addClickListener(e -> {
      dialog.confirm();
    });
    cancelButton.addClickListener(e -> {
      dialog.cancel();
    });
  }

  public static DialogFooter with(AppDialog dialog, String abortText, String confirmText) {
    return new DialogFooter(dialog, abortText, confirmText);
  }

  private DialogFooter() {
    dialog = null;
  }

  private static Button createConfirmButton(String label) {
    return createButton(label, new String[]{"button-text-primary", "button-color-primary", "button-size-medium-dialog"});
  }

  private static Button createButton(String label, String[] classNames) {
    Button button = new Button(label);
    button.addClassNames(classNames);
    return button;
  }

  private static Button createCancelButton(String label) {
    return createButton(label, new String[]{"button-text"});
  }

  public AppDialog getDialog() {
    return dialog;
  }
}
