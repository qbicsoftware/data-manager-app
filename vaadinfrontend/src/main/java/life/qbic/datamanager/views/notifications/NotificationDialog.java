package life.qbic.datamanager.views.notifications;

import com.vaadin.flow.component.HasOrderedComponents;
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
  private Icon headerIcon;
  protected final HasOrderedComponents content;


  public NotificationDialog() {
    addClassName("notification-dialog");
    title = new H2("");
    title.addClassName("title");
    headerIcon = VaadinIcon.INFO_CIRCLE.create();
    headerIcon.addClassName("info-icon");
    updateHeader();
    Div content = new Div();
    content.addClassName("content");
    add(content);
    this.content = content;
  }

  private void updateHeader() {
    setHeader(new Span(headerIcon, title));
  }

  protected void setHeaderIcon(Icon icon) {
    this.headerIcon = icon;
    updateHeader();
  }

  protected void setTitle(String text) {
    title.setText(text);
    updateHeader();
  }
}
