package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import java.util.Objects;
import org.springframework.lang.NonNull;

/**
 * <b>Simple Dialog</b>
 *
 * <p>A reusable dialog component can be configured to execute an {@link Action} for confirmation
 * or an cancel operation.</p>
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

  private transient Action confirmAction;
  private transient Action cancelAction;
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

  public static SimpleDialog small() {
    return new SimpleDialog(new LayoutSmall());
  }

  public static SimpleDialog medium() {
    return new SimpleDialog(new LayoutMedium());
  }

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

  public void confirm() {
    if (userInput != null) {
      var validation = Objects.requireNonNull(userInput.validate());
      actOnValidation(validation);
    }
  }

  private void actOnValidation(@NonNull InputValidation validation) {
    if (validation.hasPassed()) {
      confirmAction.execute();
    }
  }

  public void cancel() {
    if (cancelAction != null) {
      cancelAction.execute();
    }
  }

  public boolean hasChanges() {
    return userInput != null && userInput.hasChanges();
  }

  public void registerConfirmAction(Action confirmAction) {
    this.confirmAction = Objects.requireNonNull(confirmAction);
  }

  public void registerCancelAction(Action cancelAction) {
    this.cancelAction = Objects.requireNonNull(cancelAction);
  }

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
      return new String[] {"small-dialog"};
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
      return new String[] {"large-dialog"};
    }
  }

}
