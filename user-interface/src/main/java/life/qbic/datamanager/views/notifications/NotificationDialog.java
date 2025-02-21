package life.qbic.datamanager.views.notifications;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.dom.Style.Display;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;

/**
 * A dialog notifying the user of some event.
 * <p>
 * By default, this dialog comes with an icon and a text in the header. You can modify the header
 * text with {@link #withTitle}. When setting the icon with {@link #withHeaderIcon}, you can modify
 * the color of the icon by assigning the following css classes:
 * <ul>
 *   <li>success-icon</li>
 *   <li>warning-icon</li>
 *   <li>error-icon</li>
 *   <li>info-icon</li>
 * </ul>
 * The dialog itself is assigned a corresponding CSS class
 * <ul>
 *   <li>success-dialog</li>
 *   <li>warning-dialog</li>
 *   <li>error-dialog</li>
 *   <li>info-dialog</li>
 * </ul>
 */
public class NotificationDialog extends ConfirmDialog implements BeforeLeaveObserver {

  private final H2 title;
  private final NotificationLevel level;
  protected final Div layout;
  private Icon headerIcon;
  protected Component content;

  protected NotificationDialog(NotificationLevel level) {
    addClassName("notification-dialog");
    addClassName(switch (level) {
      case SUCCESS -> "success-dialog";
      case WARNING -> "warning-dialog";
      case ERROR -> "error-dialog";
      case INFO -> "info-dialog";
    });
    this.level = requireNonNull(level, "level must not be null");

    var defaultTitle = switch (level) {
      case SUCCESS -> "Success";
      case WARNING -> "Warning";
      case ERROR -> "Error";
      case INFO -> "Please note";
    };
    title = new H2(defaultTitle);
    title.addClassName("title");
    withHeaderIcon(typeBasedHeaderIcon(this.level));
    updateHeader();

    layout = new Div();
    layout.addClassName("content");
    this.content = new Div();
    layout.add(this.content);
    setText(layout);
    setConfirmText("Okay");
  }

  protected static Icon typeBasedHeaderIcon(NotificationLevel notificationLevel) {
    var iconCssClass = switch (notificationLevel) {
      case SUCCESS -> "success-icon";
      case WARNING -> "warning-icon";
      case ERROR -> "error-icon";
      case INFO -> "info-icon";
    };
    var icon = switch (notificationLevel) {
      case SUCCESS -> VaadinIcon.CHECK.create();
      case WARNING -> VaadinIcon.WARNING.create();
      case ERROR -> VaadinIcon.CLOSE_CIRCLE.create();
      case INFO -> VaadinIcon.INFO_CIRCLE.create();
    };
    icon.addClassName(iconCssClass);
    return icon;
  }

  /**
   * Creates a new notification dialog
   *
   * @return a notification dialog showing a success notification
   */
  public static NotificationDialog successDialog() {
    return new NotificationDialog(NotificationLevel.SUCCESS);
  }

  /**
   * Creates a new notification dialog
   *
   * @return a notification dialog showing a warning notification
   */
  public static NotificationDialog warningDialog() {
    return new NotificationDialog(NotificationLevel.WARNING);
  }

  /**
   * Creates a new notification dialog
   *
   * @return a notification dialog showing an error notification
   */
  public static NotificationDialog errorDialog() {
    return new NotificationDialog(NotificationLevel.ERROR);
  }

  /**
   * Creates a new notification dialog
   *
   * @return a notification dialog showing an info notification
   */
  public static NotificationDialog infoDialog() {
    return new NotificationDialog(NotificationLevel.INFO);
  }

  private void updateHeader() {
    setHeader(new Span(headerIcon, title));
  }

  /**
   * Changes the header icon to the icon specified.
   *
   * @param icon the icon to display in the header
   * @param <T>
   * @return a modified notification dialog
   */
  public <T extends NotificationDialog> T withHeaderIcon(Icon icon) {
    this.headerIcon = icon;
    updateHeader();
    return (T) this;
  }

  /**
   * Changes the title of the dialog. Does not touch the icon.
   *
   * @param text the title of the dialog
   * @param <T>
   * @return a dialog with the provided title
   */
  public <T extends NotificationDialog> T withTitle(String text) {
    title.setText(text);
    updateHeader();
    return (T) this;
  }

  /**
   * Sets the content of the dialog.
   * <p>
   * The content can be any {@link Component}. Previous content is removed from the dialog when
   * calling this method. The content provided must not be null but can be any empty component.
   *
   * @param content the new content of the dialog
   * @param <T>
   * @return the modified dialog
   */
  public <T extends NotificationDialog> T withContent(Component content) {
    if (this.content != null) {
      this.content.removeFromParent();
    }
    this.content = requireNonNull(content, "content must not be null");
    layout.removeAll();
    layout.add(this.content);
    return (T) this;
  }

  /**
   * Sets the content of the dialog.
   * <p>
   * The content can be any number of {@link Component}s. At least one component is required.
   * <p>
   * Previous content is removed from the dialog when calling this method.
   *
   * @param content the new content of the dialog
   * @param <T>
   * @return the modified dialog
   */
  public <T extends NotificationDialog> T withContent(Component... content) {
    if (content.length <= 0) {
      throw new IllegalArgumentException("Content must have at least one element");
    }
    Div hiddenCollectionDiv = new Div(content);
    hiddenCollectionDiv.getStyle().setDisplay(Display.CONTENTS);
    return withContent(hiddenCollectionDiv);
  }

  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    this.close();
  }
}
