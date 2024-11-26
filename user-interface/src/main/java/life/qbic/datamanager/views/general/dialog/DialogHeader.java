package life.qbic.datamanager.views.general.dialog;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import java.util.Objects;

/**
 * <b>Dialog Header</b>
 *
 * <p>A pre-formatted dialog header that shall be used as header for dialogs.</p>
 *
 * @since 1.7.0
 */
public class DialogHeader extends Div {

  private final AppDialog dialog;

  private final Div iconContainer;

  private DialogHeader() {
    dialog = null;
    iconContainer = null;
  }

  private DialogHeader(AppDialog dialog, String title) {
    addClassNames("flex-horizontal", "gap-04", "flex-align-items-center");
    this.dialog = Objects.requireNonNull(dialog);
    this.iconContainer = new Div();
    iconContainer.setVisible(false);
    var textContainer = new Div();
    textContainer.setText(title);
    textContainer.addClassNames("heading-3", "dialog-header-text-color");
    add(iconContainer, textContainer);
    dialog.setHeader(this);
  }

  public static DialogHeader with(AppDialog dialog, String title) {
    return new DialogHeader(dialog, title);
  }

  public static DialogHeader withIcon(AppDialog dialog, String title, Icon icon) {
    var header = new DialogHeader(dialog, title);
    header.setIcon(icon);
    header.displayIcon();
    return header;
  }

  private void setIcon(Icon icon) {
    iconContainer.removeAll();
    iconContainer.add(icon);
  }

  private void displayIcon() {
    iconContainer.setVisible(true);
  }

}
