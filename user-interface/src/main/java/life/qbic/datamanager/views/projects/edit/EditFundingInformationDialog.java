package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.ValidationException;
import life.qbic.datamanager.views.events.FundingInformationUpdateEvent;
import life.qbic.datamanager.views.events.ProjectDesignUpdateEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.datamanager.views.strategy.DialogClosingStrategy;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EditFundingInformationDialog extends DialogWindow {

  private DialogClosingStrategy noChangesClosingStrategy;
  private DialogClosingStrategy warningClosingStrategy;

  private FundingInformationForm form;

  public EditFundingInformationDialog(ProjectInformation project) {
    super();
    addClassName("large-dialog");
    var content = new Div();
    content.addClassName("horizontal-list");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    setHeaderTitle("Funding Information");
    form = new FundingInformationForm();
    form.setContent(project);
    content.add(form);
    add(content);
  }

  public void setDefaultStrategy(DialogClosingStrategy strategy) {
    this.noChangesClosingStrategy = strategy;
  }

  public void setWarningStrategy(DialogClosingStrategy strategy) {
    this.warningClosingStrategy = strategy;
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    try {
      var projectInfo = form.fromUserInput();
      fireEvent(new FundingInformationUpdateEvent(this, true, projectInfo));
    } catch (ValidationException e) {
      // Do nothing, the user needs to correct the input
    }
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    if (form.hasChanges() && warningClosingStrategy != null) {
      warningClosingStrategy.execute();
    } else if (noChangesClosingStrategy != null) {
      noChangesClosingStrategy.execute();
    }
  }

  public void addUpdateEventListener(ComponentEventListener<ProjectDesignUpdateEvent> listener) {
    addListener(ProjectDesignUpdateEvent.class, listener);
  }

}
