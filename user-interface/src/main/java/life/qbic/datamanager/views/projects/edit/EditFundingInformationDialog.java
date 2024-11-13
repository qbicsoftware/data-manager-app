package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.function.SerializablePredicate;
import java.util.Objects;
import life.qbic.datamanager.views.events.FundingInformationUpdateEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.funding.BoundFundingField;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.general.funding.FundingInputForm;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.datamanager.views.strategy.dialog.DialogClosingStrategy;

/**
 * <b>Funding Information Dialog</b>
 * <p>
 * Dialog that is displayed to the user, when they want to edit funding information.
 *
 * @since 1.6.0
 */
public class EditFundingInformationDialog extends DialogWindow {

  private DialogClosingStrategy noChangesClosingStrategy;
  private DialogClosingStrategy warningClosingStrategy;

  private FundingInputForm form;

  private ProjectInformation projectInformation;

  public EditFundingInformationDialog(ProjectInformation project) {
    super();
    this.projectInformation = Objects.requireNonNull(project);
    this.noChangesClosingStrategy = DefaultClosingStrategy.createDefaultStrategy(this);
    this.warningClosingStrategy = DefaultClosingStrategy.createDefaultStrategy(this);

    addClassName("large-dialog");
    var content = new Div();
    content.addClassName("horizontal-list");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    setHeaderTitle("Edit Funding Information");
    var fundingField = FundingField.createHorizontal("Funding");
    form = FundingInputForm.create(new BoundFundingField(fundingField,
        Validator.from(incompletePredicate(),
            "Please provide complete information for both, grant and grand ID.")));
    form.setContent(project.getFundingEntry().orElse(new FundingEntry("", "")));
    content.add(form);
    add(content);
  }

  private static SerializablePredicate<FundingEntry> incompletePredicate() {
    return (FundingEntry entry) ->
        !((entry.getLabel().isBlank() && !entry.getReferenceId().isBlank()) ||
            (!entry.getLabel().isBlank() && entry.getReferenceId().isBlank()));
  }

  public void setDefaultCancelStrategy(DialogClosingStrategy strategy) {
    this.noChangesClosingStrategy = strategy;
  }

  public void setCancelWithoutSaveStrategy(DialogClosingStrategy strategy) {
    this.warningClosingStrategy = strategy;
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    try {
      var fundingEntry = form.fromUserInput();
      projectInformation.setFundingEntry(fundingEntry);
      fireEvent(new FundingInformationUpdateEvent(this, true, projectInformation));
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

  public void addUpdateEventListener(
      ComponentEventListener<FundingInformationUpdateEvent> listener) {
    addListener(FundingInformationUpdateEvent.class, listener);
  }

}
