package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.datamanager.views.projects.project.access.ProjectAccessComponent.ProjectUser;

/**
 * Warns a user that the user will be removed from the project
 * <p>
 * This dialog is to be shown when a user is removed from a project within the
 * {@link ProjectAccessComponent}
 */
public class ProjectUserRemovalConfirmationNotification extends NotificationDialog {

  public ProjectUserRemovalConfirmationNotification(ProjectUser projectUser) {
    customizeHeader();
    content.add(new Span(
        "Are you sure you want to remove the user %s from the project?".formatted(
            projectUser.userName())));
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader() {
    Icon warningIcon = new Icon(VaadinIcon.WARNING);
    warningIcon.setClassName("warning-icon");
    setTitle("Remove user from project");
    setHeaderIcon(warningIcon);
  }
}
