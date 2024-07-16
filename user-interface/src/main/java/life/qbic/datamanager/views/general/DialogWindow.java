package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.server.Command;

/**
 * <b>Dialog to create something</b>
 *
 * <p>Can be used to display the necessary inputs to create projects etc.
 * Every DialogWindow has two buttons: One to confirm, one to cancel the DialogWindow without making
 * changes. Button titles can be set in the respective Class using DialogWindow.</p>
 *
 * @since 1.0.0
 */
public abstract class DialogWindow extends Dialog {

  protected final Button confirmButton = new Button("Confirm");
  protected final Button cancelButton = new Button("Cancel");

  protected DialogWindow() {
    this.addClassName("dialog-window");
    confirmButton.addClassName("primary");
    cancelButton.setThemeName("tertiary");
    setCloseOnOutsideClick(false);
    setCloseOnEsc(false);
    getFooter().add(cancelButton, confirmButton);
    cancelButton.addClickListener(this::onCancelClicked);
    confirmButton.addClickListener(this::onConfirmClicked);
  }

  protected void specifyCancelShortcuts(Command onCreationCanceled) {
      setCloseOnOutsideClick(false);
      setCloseOnEsc(false);
      Shortcuts.addShortcutListener(this,
          onCreationCanceled, Key.ESCAPE);
  }

  /**
   * Overwrite to change what happens on confirm button clicked
   *
   * @param clickEvent
   */
  protected abstract void onConfirmClicked(ClickEvent<Button> clickEvent);

  /**
   * Overwrite to change what happens on cancel button clicked.
   *
   * @param clickEvent
   */
  protected abstract void onCancelClicked(ClickEvent<Button> clickEvent);


  /**
   * Sets the label of the button that confirms the finished Dialog Window
   * @param confirmLabel
   */
  public void setConfirmButtonLabel(String confirmLabel){
    confirmButton.setText(confirmLabel);
    confirmButton.setAriaLabel(confirmLabel);
  }

  /**
   * Sets the label of the cancel that cancels the Dialog Window
   * @param cancelLabel
   */
  public void setCancelButtonLabel(String cancelLabel){
    cancelButton.setText(cancelLabel);
    cancelButton.setAriaLabel(cancelLabel);
  }

}
