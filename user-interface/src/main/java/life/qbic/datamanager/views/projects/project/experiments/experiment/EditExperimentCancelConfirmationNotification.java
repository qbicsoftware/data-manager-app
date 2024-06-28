package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that they are about to cancel experiment editing
 * <p>
 * This dialog is to be shown when experiment editing is canceled via the Esc key or the Cancel button
 */
public class EditExperimentCancelConfirmationNotification extends NotificationDialog {

  public EditExperimentCancelConfirmationNotification() {
    customizeHeader();
    content.add(new Span(
        "You will lose all the changes made to this experiment."));
    setCancelable(true);
    setConfirmText("Discard changes");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setTitle("Discard experiment changes?");
    setHeaderIcon(errorIcon);
  }
}
