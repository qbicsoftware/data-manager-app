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

  public MeasurementDeletionConfirmationNotification(String title, int amount) {
    super(Type.INFO);
    customizeHeader(title);
    layout.add(new Span(
        "Are you sure you want to delete %s measurements?".formatted(String.valueOf(amount))));
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader(String title) {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    withTitle(title);
    withHeaderIcon(errorIcon);
  }
}
