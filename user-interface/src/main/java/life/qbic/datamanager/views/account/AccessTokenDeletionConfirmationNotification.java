package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that the personal access token will be deleted and cannot be used
 * <p>
 * This dialog is to be shown when PAT delection is triggered by the user.
 */
public class AccessTokenDeletionConfirmationNotification extends NotificationDialog {

  public AccessTokenDeletionConfirmationNotification() {
    super(Type.INFO);
    customizeHeader();
    withContent(new Span(
        "Deleting this Personal Access Token will make it unusable. Proceed?"));
    setCancelable(true);
    setConfirmText("Confirm");
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    withTitle("Personal Access Token will be deleted");
    withHeaderIcon(errorIcon);
  }
}
