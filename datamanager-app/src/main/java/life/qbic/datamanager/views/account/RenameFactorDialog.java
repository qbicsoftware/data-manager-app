package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import java.io.Serial;
import java.util.function.Function;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.identity.domain.model.mfa.SecondFactor;

/**
 * <b>Renames one authenticator app</b>
 *
 * <p>The label is the only part of a factor a user can change after enrolment, and the only thing that
 * tells three enrolled apps apart in the settings list. A typo made during setup would otherwise be
 * permanent — and the obvious workaround, removing and re-enrolling, throws away a working credential for
 * a cosmetic problem.</p>
 *
 * <p>The field is prefilled with the current name, so "rename" doubles as "check what it is called" and
 * clearing it is an explicit act rather than a default.</p>
 *
 * @since 1.0.0
 */
public class RenameFactorDialog {

  @Serial
  private static final long serialVersionUID = 1L;

  private final AppDialog dialog;

  /**
   * @param currentName the factor's label now
   * @param rename      applies the new label; returns {@code false} when it was refused
   */
  public RenameFactorDialog(String currentName, Function<String, Boolean> rename) {
    requireNonNull(currentName, "currentName must not be null");
    requireNonNull(rename, "rename must not be null");

    dialog = AppDialog.small();
    DialogHeader.with(dialog, "Name this authenticator app");

    TextField name = new TextField("Name");
    name.setValue(currentName);
    name.setWidthFull();
    // Same bound as the column the label is stored in: a form that allows more than the database holds
    // fails on submit, after the user has already done the work.
    name.setMaxLength(SecondFactor.MAX_DISPLAY_NAME_LENGTH);
    name.setHelperText("Shown in this list. Use something you will recognise later, such as "
        + "\"iPhone (1Password)\".");
    name.setAutofocus(true);

    Span error = new Span();
    error.addClassName("mfa-error");
    error.getStyle().set("color", "var(--lumo-error-text-color)");
    error.setVisible(false);

    Div body = new Div();
    body.add(new Paragraph("Rename only changes the label here. The app keeps working, and its codes "
        + "stay valid."), name, error);

    DialogBody.with(dialog, body, new UserInput() {
      @Override
      public InputValidation validate() {
        error.setVisible(false);
        name.setInvalid(false);
        String chosen = name.getValue() == null ? "" : name.getValue().strip();
        if (chosen.isEmpty()) {
          // Clearing the field is not a way to "remove the name": a factor always has a label.
          error.setText("Enter a name, or cancel to keep the current one.");
          error.setVisible(true);
          name.setInvalid(true);
          return InputValidation.failed();
        }
        if (chosen.equals(currentName)) {
          // Nothing to do: close without a write rather than rewriting the same value.
          return InputValidation.passed();
        }
        if (!rename.apply(chosen)) {
          error.setText("That name could not be saved. Please try again.");
          error.setVisible(true);
          return InputValidation.failed();
        }
        return InputValidation.passed();
      }

      @Override
      public boolean hasChanges() {
        String chosen = name.getValue() == null ? "" : name.getValue().strip();
        return !chosen.equals(currentName);
      }
    });

    DialogFooter.with(dialog, "Cancel", "Save name");
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(dialog::close);
  }

  public void open() {
    dialog.open();
  }
}
