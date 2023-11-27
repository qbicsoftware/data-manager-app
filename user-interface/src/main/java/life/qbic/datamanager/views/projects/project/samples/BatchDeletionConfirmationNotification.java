package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that the samples contained in the batch will also be deleted
 * <p>
 * This dialog is to be shown when batch deletion is triggered while samples are contained within
 * the batch.
 */
public class BatchDeletionConfirmationNotification extends NotificationDialog {

  public BatchDeletionConfirmationNotification() {
    addClassName("batch-deletion-confirmation");
    customizeHeader();
    content.add(new Div(new Text(
        "Deleting this Batch will also delete the samples contained within. Proceed?")));
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setTitle("Samples within batch will be deleted");
    setHeaderIcon(errorIcon);
  }
}