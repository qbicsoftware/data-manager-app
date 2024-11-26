package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog.ConfirmEvent;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/**
 * <b>Simple Dialog</b>
 *
 * <p>A reusable dialog component can be configured to execute an {@link DialogAction} for
 * confirmation or an cancel operation.</p>
 * <p>
 * A simple dialog always checks its main component, which is of type {@link UserInput} and gets
 * validated first.
 * <p>
 * If the {@link InputValidation }
 *
 * @since 1.7.0
 */
public class SimpleDialog extends Dialog {

  private final Div header;
  private final Div body;
  private final Div footer;

  private transient DialogAction confirmDialogAction;
  private transient DialogAction cancelDialogAction;
  private transient UserInput userInput;

  private SimpleDialog(Style style) {
    addClassName("simple-dialog");
    addClassNames(style.sizes());
    header = style.header();
    body = style.body();
    footer = style.footer();
    super.getFooter().add(footer);
    super.getHeader().add(header);
    super.add(body);
  }

  /**
   * Creates a small dialog, that will not consume much of the available display. Ideal for simple
   * notifications or very sparse user input.
   *
   * @return a simple dialog in its small layout variant
   * @since 1.7.0
   */
  public static SimpleDialog small() {
    return new SimpleDialog(new LayoutSmall());
  }

  /**
   * Creates a medium dialog, that will consume more of the available display than the small dialog.
   * Ideal for moderate user input scenarios or file uploads.
   *
   * @return a simple dialog in its medium layout variant
   * @since 1.7.0
   */
  public static SimpleDialog medium() {
    return new SimpleDialog(new LayoutMedium());
  }

  /**
   * Creates a large dialog, that will consume more of the available display than the medium dialog.
   * Ideal for complex user input scenarios.
   *
   * @return a simple dialog in its large layout variant
   * @since 1.7.0
   */
  public static SimpleDialog large() {
    return new SimpleDialog(new LayoutLarge());
  }

  public void setHeader(Component header) {
    this.header.removeAll();
    this.header.add(header);
  }

  public void setBody(Component body) {
    this.body.removeAll();
    this.body.add(body);
  }

  public void setFooter(Component footer) {
    this.footer.removeAll();
    this.footer.add(footer);
  }

  /**
   * The user intends to confirm the current dialog context.
   * <p>
   * In case to user input has been defined (e.g. for a notification dialog), no validation will be
   * performed and the confirmation action directly executed
   * {@link #registerConfirmAction(DialogAction)}.
   *
   * @since 1.7.0
   */
  public void confirm() {
    if (userInput != null) {
      var validation = Objects.requireNonNull(userInput.validate());
      validation.onPassed(confirmDialogAction);
    } else {
      // no user input was defined, so nothing to validate
      confirmDialogAction.execute();
    }
  }

  /**
   * Calls the {@link DialogAction} if one has been registered with
   * {@link #registerCancelAction(DialogAction)}.
   *
   * @since 1.7.0
   */
  public void cancel() {
    if (hasChanges()) {
      var confirmDialog = createConfirmDialog(cancelDialogAction);
      confirmDialog.open();
    } else if (confirmDialogAction != null) {
      confirmDialogAction.execute();
    }
  }

  private static ConfirmDialog createConfirmDialog(DialogAction onConfirmAction) {
    var confirmDialog = new ConfirmDialog();
    confirmDialog.setConfirmButton("Discard Changes", (ConfirmEvent event) -> onConfirmAction.execute());
    confirmDialog.setConfirmButton("Continue", (ConfirmEvent event) -> confirmDialog.close());
    return confirmDialog;
  }

  /**
   * Indicates if any changes have been made in the presence of a {@link UserInput} context by the
   * user.
   * <p>
   * Will always return <code>false</code>, if no {@link UserInput} has been registered via
   * {@link #registerUserInput(UserInput)},
   *
   * @return true if changes were made since the initial state of the {@link UserInput}, else false
   * @since 1.7.0
   */
  public boolean hasChanges() {
    return userInput != null && userInput.hasChanges();
  }

  /**
   * Registers a {@link DialogAction} that will be executed after the dialog receives a confirmation
   * signal via its public method {@link #confirm()}.
   *
   * @param uponConfirmation the action to be executed after confirmation
   * @since 1.7.0
   */
  public void registerConfirmAction(DialogAction uponConfirmation) {
    this.confirmDialogAction = Objects.requireNonNull(uponConfirmation);
  }

  /**
   * Registers a {@link DialogAction} that will be executed after the dialog receives a cancellation
   * signal via its public method {@link #cancel()} }.
   *
   * @param uponCancel the action to be executed after cancellation
   * @since 1.7.0
   */
  public void registerCancelAction(DialogAction uponCancel) {
    this.cancelDialogAction = Objects.requireNonNull(uponCancel);
  }

  /**
   * Registers a {@link UserInput}, that will be validated after the user indicates to confirm the
   * current contextual task in the dialog.
   *
   * @param userInput a user input component, that presents some input data that shall be validated
   * @since 1.7.0
   */
  public void registerUserInput(UserInput userInput) {
    this.userInput = Objects.requireNonNull(userInput);
  }


  private interface Style {

    Div header();

    Div body();

    Div footer();

    String[] sizes();
  }

  private static class LayoutSmall implements Style {

    Div header = new Div();
    Div body = new Div();
    Div footer = new Div();

    LayoutSmall() {
      header.addClassNames(paddings());
      body.addClassNames(paddings());
      footer.addClassNames(paddings());
    }

    private static String[] paddings() {
      return new String[]{"padding-left-right-05", "padding-top-bottom-05"};
    }

    @Override
    public Div header() {
      return header;
    }

    @Override
    public Div body() {
      return body;
    }

    @Override
    public Div footer() {
      return footer;
    }

    public String[] sizes() {
      return new String[]{"small-dialog"};
    }
  }

  private static class LayoutMedium implements Style {

    Div header = new Div();
    Div body = new Div();
    Div footer = new Div();

    LayoutMedium() {
      header.addClassNames(paddings());
      body.addClassNames(paddings());
      footer.addClassNames(paddings());
    }

    private static String[] paddings() {
      return new String[]{"padding-left-right-05", "padding-top-bottom-05"};
    }

    public String[] sizes() {
      return new String[]{"medium-dialog"};
    }

    @Override
    public Div header() {
      return header;
    }

    @Override
    public Div body() {
      return body;
    }

    @Override
    public Div footer() {
      return footer;
    }
  }

  private static class LayoutLarge implements Style {

    Div header = new Div();
    Div body = new Div();
    Div footer = new Div();

    LayoutLarge() {
      header.addClassNames(paddings());
      body.addClassNames(paddings());
      footer.addClassNames(paddings());
    }

    private static String[] paddings() {
      return new String[]{"padding-left-right-07", "padding-top-bottom-07"};
    }

    @Override
    public Div header() {
      return header;
    }

    @Override
    public Div body() {
      return body;
    }

    @Override
    public Div footer() {
      return footer;
    }

    public String[] sizes() {
      return new String[]{"large-dialog"};
    }
  }

}
