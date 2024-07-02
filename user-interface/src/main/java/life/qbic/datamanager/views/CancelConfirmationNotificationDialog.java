package life.qbic.datamanager.views;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that they are about to cancel a dialog and lose input data
 * <p>
 * This dialog is to be shown when a dialog is canceled via the Esc key or the Cancel button
 */
public class CancelConfirmationNotificationDialog extends NotificationDialog {

  public CancelConfirmationNotificationDialog() {
    customizeHeader();
    setCancelable(true);
    setConfirmText("Discard");
  }

  public CancelConfirmationNotificationDialog withBodyText(String mainText) {
    content.add(new Span(
        mainText));
    return this;
  }

  public CancelConfirmationNotificationDialog withConfirmText(String confirmText) {
    setConfirmText(confirmText);
    return this;
  }

  public CancelConfirmationNotificationDialog withTitle(String headerText) {
    setTitle(headerText);
    return this;
  }

  private void customizeHeader() {
    Icon errorIcon = new Icon(VaadinIcon.WARNING);
    errorIcon.setClassName("warning-icon");
    setHeaderIcon(errorIcon);
  }
}
