package life.qbic.datamanager.views.projects.project;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that they are about to cancel project creation
 * <p>
 * This dialog is to be shown when project creation is canceled via the Esc key or the Cancel button
 */
public class CreateProjectCancelConfirmationNotification extends NotificationDialog {

  public CreateProjectCancelConfirmationNotification() {
    customizeHeader();
    content.add(new Span(
        "You will lose all the information entered for this project."));
    setCancelable(true);
    setConfirmText("Discard project creation");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setTitle("Discard new project creation?");
    setHeaderIcon(errorIcon);
  }
}
