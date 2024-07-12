package life.qbic.datamanager.views.projects.qualityControl;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that the file will be deleted from the server.
 * <p>
 * This dialog is to be shown when a user wants to delete a Quality Control Item.
 */
public class QCItemDeletionConfirmationNotification extends NotificationDialog {

  public QCItemDeletionConfirmationNotification() {
    super(Type.INFO);
    customizeHeader();
    setContent(new Span(
        "Are you sure you want to delete this file?"));
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setTitle("Quality control will be deleted");
    setHeaderIcon(errorIcon);
  }
}
