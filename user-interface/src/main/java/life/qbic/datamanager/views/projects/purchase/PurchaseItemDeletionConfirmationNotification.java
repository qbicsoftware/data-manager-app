package life.qbic.datamanager.views.projects.purchase;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that the file will be deleted from the server.
 * <p>
 * This dialog is to be shown when a user wants to delete an Offer item.
 */
public class PurchaseItemDeletionConfirmationNotification extends NotificationDialog {

  public PurchaseItemDeletionConfirmationNotification() {
    super(Type.INFO);
    customizeHeader();
    withContent(new Span(
        "Are you sure you want to delete this offer?"));
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    withTitle("Offer will be deleted");
    withHeaderIcon(errorIcon);
  }
}
