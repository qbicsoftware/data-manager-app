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

  public BatchDeletionConfirmationNotification(int sampleCount) {
    addClassName("batch-deletion-confirmation");
    customizeHeader();
    customizeContent(sampleCount);
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setTitle("Samples within batch will be deleted");
    setHeaderIcon(errorIcon);
  }

  private void customizeContent(int sampleCount) {
    content.add(new Div(new Text(String.format(
        "Deleting this Batch will also delete the %s samples contained within. Proceed?",
        sampleCount))));
  }
}
