package life.qbic.datamanager.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.notifications.NotificationDialog;

/**
 * Warns the user that they are about to cancel a dialog and lose input data
 * <p>
 * This dialog is to be shown when a dialog is canceled via the Esc key or the Cancel button
 */
public class CancelConfirmationNotificationDialog extends NotificationDialog {

  public CancelConfirmationNotificationDialog() {
    super(Type.WARNING);
    setCancelable(true);
    setCancelText("Continue Editing");
    Button redButton = new Button("Discard");
    redButton.addClassName("danger");
    setConfirmButton(redButton);
  }

  public CancelConfirmationNotificationDialog withBodyText(String mainText) {
    setContent(new Span(mainText));
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
}
