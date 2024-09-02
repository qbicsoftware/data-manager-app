package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.datamanager.views.notifications.NotificationLevel;

/**
 * Warns the user that the samples contained in the batch will also be deleted
 * <p>
 * This dialog is to be shown when batch deletion is triggered while samples are contained within
 * the batch.
 */
public class BatchDeletionConfirmationNotification extends NotificationDialog {

  public BatchDeletionConfirmationNotification() {
    super(NotificationLevel.ERROR);
    withTitle("Samples within batch will be deleted");
    withContent(new Span(
        "Deleting this Batch will also delete the samples contained within. Proceed?"));
    setCancelable(true);
    setConfirmText("Confirm");
  }
}
