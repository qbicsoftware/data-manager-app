package life.qbic.datamanager.views.projects.purchase;

import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that the file will be deleted from the server.
 * <p>
 * This dialog is to be shown when a user wants to delete an Offer item.
 */
public class PurchaseItemDeletionConfirmationNotification extends NotificationDialog {

  public PurchaseItemDeletionConfirmationNotification() {
    super(Type.WARNING);
    withTitle("Offer will be deleted");
    withContent(new Span(
        "Are you sure you want to delete this offer?"));
    setCancelable(true);
    setConfirmText("Confirm");
  }

}
