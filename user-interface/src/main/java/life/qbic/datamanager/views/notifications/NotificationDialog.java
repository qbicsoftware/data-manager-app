package life.qbic.datamanager.views.notifications;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * A dialog notifying the user of some event.
 * <p>
 * By default, this dialog comes with an icon and a text in the header. You can modify the header
 * text with {@link #setTitle}. When setting the icon with {@link #setHeaderIcon}, you can modify
 * the color of the icon by assigning the following css classes:
 * <ul>
 *   <li>error-icon</li>
 *   <li>warning-icon</li>
 *   <li>info-icon</li>
 * </ul>
 * <p>
 * The content of the dialog can be accessed by extending classes in the final field {@link #content}.
 */
public class NotificationDialog extends ConfirmDialog {

  private final H2 title;
  private final Type type;
  protected final Div layout;
  private Icon headerIcon;
  protected Component content;


  protected enum Type {
    SUCCESS, WARNING, ERROR, INFO
  }

  protected NotificationDialog(Type type) {
    addClassName("notification-dialog");
    addClassName(switch (type) {
      case SUCCESS -> "success-dialog";
      case WARNING -> "warning-dialog";
      case ERROR -> "error-dialog";
      case INFO -> "info-dialog";
    });
    this.type = requireNonNull(type, "type must not be null");

    title = new H2("Please note");
    title.addClassName("title");
    setHeaderIcon(typeBasedHeaderIcon(this.type));
    updateHeader();

    layout = new Div();
    layout.addClassName("content");
    this.content = new Div();
    layout.add(this.content);
    add(layout);
    setConfirmText("Okay");
  }

  protected static Icon typeBasedHeaderIcon(Type newType) {
    var iconCssClass = switch (newType) {
      case SUCCESS -> "success-icon";
      case WARNING -> "warning-icon";
      case ERROR -> "error-icon";
      case INFO -> "info-icon";
    };
    var icon = switch (newType) {
      case SUCCESS -> VaadinIcon.CHECK.create();
      case WARNING -> VaadinIcon.WARNING.create();
      case ERROR -> VaadinIcon.CLOSE_CIRCLE.create();
      case INFO -> VaadinIcon.INFO_CIRCLE.create();
    };
    icon.addClassName(iconCssClass);
    return icon;
  }

  public static NotificationDialog successDialog() {
    return new NotificationDialog(Type.SUCCESS);
  }

  public static NotificationDialog warningDialog() {
    return new NotificationDialog(Type.WARNING);
  }

  public static NotificationDialog errorDialog() {
    return new NotificationDialog(Type.ERROR);
  }

  public static NotificationDialog infoDialog() {
    return new NotificationDialog(Type.INFO);
  }

  private void updateHeader() {
    setHeader(new Span(headerIcon, title));
  }

  public void setHeaderIcon(Icon icon) {
    this.headerIcon = icon;
    updateHeader();
  }

  public void setTitle(String text) {
    title.setText(text);
    updateHeader();
  }

  public NotificationDialog setContent(Component content) {
    if (this.content != null) {
      this.content.removeFromParent();
    }
    this.content = requireNonNull(content, "content must not be null");
    layout.removeAll();
    layout.add(content);
    return this;
  }
}
