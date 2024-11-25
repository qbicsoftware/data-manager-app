package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.html.Div;
import java.util.Objects;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class DialogHeader extends Div {

  private final SimpleDialog dialog;

  private DialogHeader() {
    dialog = null;
  }

  private DialogHeader(SimpleDialog dialog, String title) {
    this.dialog = Objects.requireNonNull(dialog);
    setText(title);
    addClassNames("heading-3", "dialog-header-text-color");
    dialog.setHeader(this);
  }

  public static DialogHeader with(SimpleDialog dialog, String title) {
    return new DialogHeader(dialog, title);
  }

}
