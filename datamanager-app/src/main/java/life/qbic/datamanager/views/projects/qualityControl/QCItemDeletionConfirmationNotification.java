package life.qbic.datamanager.views.projects.qualityControl;

import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.datamanager.views.notifications.NotificationLevel;

/**
 * Warns the user that the file will be deleted from the server.
 * <p>
 * This dialog is to be shown when a user wants to delete a Quality Control Item.
 */
public class QCItemDeletionConfirmationNotification extends NotificationDialog {

  public QCItemDeletionConfirmationNotification() {
    super(NotificationLevel.WARNING);
    withTitle("Quality control will be deleted");
    withContent(new Span(
        "Are you sure you want to delete this file?"));
    setCancelable(true);
    setConfirmText("Confirm");
  }
}
