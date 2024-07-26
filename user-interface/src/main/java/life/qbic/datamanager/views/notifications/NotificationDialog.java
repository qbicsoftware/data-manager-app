package life.qbic.datamanager.views.notifications;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.general.ConfirmDialog;

/**
 * A dialog notifying the user of some event.
 * <p>
 * By default, this dialog comes with an icon and a text in the header. You can modify the header
 * text with {@link #withTitle}. When setting the icon with {@link #withHeaderIcon}, you can modify
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

    var defaultTitle = switch (type) {
      case SUCCESS -> "Success";
      case WARNING -> "Warning";
      case ERROR -> "Error";
      case INFO -> "Please note";
    };
    title = new H2(defaultTitle);
    title.addClassName("title");
    withHeaderIcon(typeBasedHeaderIcon(this.type));
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

  public <T extends NotificationDialog> T withHeaderIcon(Icon icon) {
    this.headerIcon = icon;
    updateHeader();
    return (T) this;
  }

  public <T extends NotificationDialog> T withTitle(String text) {
    title.setText(text);
    updateHeader();
    return (T) this;
  }

  public <T extends NotificationDialog> T withContent(Component content) {
    if (this.content != null) {
      this.content.removeFromParent();
    }
    this.content = requireNonNull(content, "content must not be null");
    layout.removeAll();
    layout.add(content);
    return (T) this;
  }

  public <T extends NotificationDialog> T withHtmlContent(String htmlContent) {
    return withContent(new Html("<div>%s</div>".formatted(htmlContent)));
  }
}
