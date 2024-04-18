package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;

/**
 * <b>Wizard Dialog Window</b>
 * <p>
 * The wizard dialog window offers a third button type to the classical {@link DialogWindow}: the
 * "Finish" button.
 * <p>
 * This is often required, when a user process is happening in the dialog and a finish indicates the
 * process being done and the user can close the window.
 * <p>
 * The default appearance is the confirm button is shown, and the finish button can be toggled when
 * called explicitly via {@link WizardDialogWindow#showFinished()} and the finish button is disabled
 * by default.
 *
 * @since 1.0.0
 */
public abstract class WizardDialogWindow extends DialogWindow {

  protected final Button finishButton;

  protected WizardDialogWindow() {
    super();
    finishButton = new Button("Finish");
    finishButton.addClassName("primary");
    getFooter().add(finishButton);
    showConfirm();
    disableFinishButton();
    finishButton.addClickListener(this::onFinishClicked);
  }

  /**
   * Defines the action to be done when the finish button has been clicked.
   *
   * @param clickEvent the click event
   * @since 1.0.0
   */
  protected void onFinishClicked(ClickEvent<Button> clickEvent) {
    this.close();
  }

  public void showFinished() {
    this.confirmButton.setVisible(false);
    this.finishButton.setVisible(true);
  }

  public void showConfirm() {
    this.confirmButton.setVisible(true);
    this.finishButton.setVisible(false);
  }

  public void disableFinishButton() {
    this.finishButton.setEnabled(false);
  }

  public void enableFinishButton() {
    this.finishButton.setEnabled(true);
  }


}
