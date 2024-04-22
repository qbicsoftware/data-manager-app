package life.qbic.datamanager.views.projects.project.measurements;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that measurements will be deleted
 * <p>
 * This dialog is to be shown when measurement deletion is triggered
 */
public class MeasurementDeletionConfirmationNotification extends NotificationDialog {

  public MeasurementDeletionConfirmationNotification() {
    customizeHeader();
    content.add(new Span(
        "Are you sure you want to delete the selected measurements?"));
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setTitle("Measurements will be deleted");
    setHeaderIcon(errorIcon);
  }
}
