package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that they are about to cancel experiment creation
 * <p>
 * This dialog is to be shown when experiment creation is canceled via the Esc key or the Cancel button
 */
public class CreateExperimentCancelConfirmationNotification extends NotificationDialog {

  public CreateExperimentCancelConfirmationNotification() {
    customizeHeader();
    content.add(new Span(
        "You will lose all the information entered for this experiment."));
    setCancelable(true);
    setConfirmText("Discard experiment creation");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setTitle("Discard new experiment creation?");
    setHeaderIcon(errorIcon);
  }
}
