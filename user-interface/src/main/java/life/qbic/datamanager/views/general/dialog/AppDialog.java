package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import life.qbic.datamanager.views.general.icon.IconFactory;

/**
 * <b>App Dialog</b>
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
public class AppDialog extends Dialog {

  public static final String PADDING_LEFT_RIGHT_07 = "padding-left-right-07";
  public static final String PADDING_TOP_BOTTOM_05 = "padding-top-bottom-05";
  private final Div header;
  private final Div body;
  private final Div footer;

  private transient DialogAction confirmDialogAction;
  private transient DialogAction cancelDialogAction;
  private transient UserInput userInput;

  private AppDialog(Style style) {
    addClassName("dialog-app");
    addClassNames(style.sizes());
    header = style.header();
    body = style.body();
    footer = style.footer();
    super.getFooter().add(footer);
    super.getHeader().add(header);
    super.add(body);
    setModal(true);
    setCloseOnOutsideClick(false);
    setCloseOnEsc(false);
  }

  /**
   * Creates a small dialog, that will not consume much of the available display. Ideal for simple
   * notifications or very sparse user input.
   *
   * @return a simple dialog in its small layout variant
   * @since 1.7.0
   */
  public static AppDialog small() {
    return new AppDialog(new LayoutSmall());
  }

  /**
   * Creates a medium dialog, that will consume more of the available display than the small dialog.
   * Ideal for moderate user input scenarios or file uploads.
   *
   * @return a simple dialog in its medium layout variant
   * @since 1.7.0
   */
  public static AppDialog medium() {
    return new AppDialog(new LayoutMedium());
  }

  /**
   * Creates a large dialog, that will consume more of the available display than the medium dialog.
   * Ideal for complex user input scenarios.
   *
   * @return a simple dialog in its large layout variant
   * @since 1.7.0
   */
  public static AppDialog large() {
    return new AppDialog(new LayoutLarge());
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
      validation.ifPassed(confirmDialogAction);
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
    } else if (cancelDialogAction != null) {
      cancelDialogAction.execute();
    }
  }

  private static AppDialog createConfirmDialog(DialogAction onConfirmAction) {
    var confirmDialog = AppDialog.small();
    life.qbic.datamanager.views.general.dialog.DialogHeader.withIcon(confirmDialog, "Discard changes?",
        IconFactory.warningIcon());
    DialogBody.withoutUserInput(confirmDialog, new Div("By aborting the editing process and closing the dialog, you will loose all information entered."));
    life.qbic.datamanager.views.general.dialog.DialogFooter.with(confirmDialog, "Continue editing", "Discard changes" );
    confirmDialog.registerConfirmAction(onConfirmAction);
    confirmDialog.registerCancelAction(confirmDialog::close);
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
      return new String[]{PADDING_LEFT_RIGHT_07, PADDING_TOP_BOTTOM_05};
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
      return new String[]{"dialog-small"};
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
      return new String[]{PADDING_LEFT_RIGHT_07, PADDING_TOP_BOTTOM_05};
    }

    public String[] sizes() {
      return new String[]{"dialog-medium"};
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
      return new String[]{PADDING_LEFT_RIGHT_07, PADDING_TOP_BOTTOM_05};
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
      return new String[]{"dialog-large"};
    }
  }

}
