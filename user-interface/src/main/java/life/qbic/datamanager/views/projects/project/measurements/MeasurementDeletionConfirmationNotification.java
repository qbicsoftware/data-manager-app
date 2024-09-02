package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.datamanager.views.notifications.NotificationLevel;

/**
 * Warns the user that measurements will be deleted
 * <p>
 * This dialog is to be shown when measurement deletion is triggered
 */
public class MeasurementDeletionConfirmationNotification extends NotificationDialog {

  public MeasurementDeletionConfirmationNotification(String title, int amount) {
    super(NotificationLevel.WARNING);
    withTitle(title);
    withContent(new Span(
        "Are you sure you want to delete %s measurements?".formatted(String.valueOf(amount))));
    setCancelable(true);
    setConfirmText("Confirm");
  }
}
