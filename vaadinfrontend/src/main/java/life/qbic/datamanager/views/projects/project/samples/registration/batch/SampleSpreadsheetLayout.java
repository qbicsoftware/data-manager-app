package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationSpreadsheet.InvalidSpreadsheetInput;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationSpreadsheet.NGSRowDTO;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;

/**
 * <b>Sample Spreadsheet Layout</b>
 * <p>
 * Layout which is responsible for hosting the spreadsheet in which the metadata information
 * associated for each sample will be provided>
 * </p>
 */
class SampleSpreadsheetLayout extends Div {

  private final Span sampleInformationHeader = new Span("Sample Information");
  private final Span batchRegistrationInstruction = new Span();
  private final Span errorInstructionSpan = new Span();
  private final Span batchName = new Span();
  private final Span experimentName = new Span();
  private final SampleRegistrationSpreadsheet sampleRegistrationSpreadsheet = new SampleRegistrationSpreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button addRowButton = new Button("Add Row");
  public final Button deleteRowButton = new Button("Delete Row");
  public final Button backButton = new Button("Back");
  public final Button registerButton = new Button("Register");
  private final SampleInformationLayoutHandler sampleInformationLayoutHandler;
  private ExperimentId experiment;
  private boolean isPrefilled = false;

  //The spreadsheet breaks if the Notification is generated via an ApplicationException
  private final StyledNotification lastRowDeletionNotification = new StyledNotification(
      new ErrorMessage("Can't delete last row",
          "At least one row has to remain in the spreadsheet"));

  SampleSpreadsheetLayout() {
    initContent();
    this.addClassName("batch-content");
    sampleInformationLayoutHandler = new SampleInformationLayoutHandler();
  }

  private void initContent() {
    initHeaderAndInstruction();
    Div sampleSpreadSheetContainer = new Div();
    sampleSpreadSheetContainer.addClassName("sample-spreadsheet");
    sampleSpreadSheetContainer.add(sampleRegistrationSpreadsheet);
    add(sampleSpreadSheetContainer);
    styleSampleRegistrationSpreadSheet();
    initButtonLayout();
    addComponentAsFirst(lastRowDeletionNotification);
  }

  private void initHeaderAndInstruction() {
    sampleInformationHeader.setClassName("title");
    Span instructionSpan = new Span();
    instructionSpan.add("Please register your samples for experiment: ");
    instructionSpan.add(experimentName);
    experimentName.setClassName("bold");
    instructionSpan.add(" in batch: ");
    instructionSpan.add(batchName);
    batchRegistrationInstruction.add(instructionSpan);
    batchName.addClassName("bold");
    add(sampleInformationHeader);
    add(batchRegistrationInstruction);
    add(errorInstructionSpan);
  }

  private void initButtonLayout() {
    Span sampleInformationButtons = new Span();
    sampleInformationButtons.addClassName("buttons");
    addRowButton.addClickListener(
        (ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> sampleRegistrationSpreadsheet.addRow());
    deleteRowButton.addClickListener(
        event -> {
          if (!isLastRow()) {
            sampleRegistrationSpreadsheet.deleteRow(
                sampleRegistrationSpreadsheet.getRows() - 1);
          } else {
            lastRowDeletionNotification.open();
          }
        });
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sampleInformationButtons.add(backButton, addRowButton, deleteRowButton, cancelButton,
        registerButton);
    add(sampleInformationButtons);
  }

  private boolean isLastRow() {
    return sampleRegistrationSpreadsheet.getRows() <= 2;
  }

  private void styleSampleRegistrationSpreadSheet() {
    sampleRegistrationSpreadsheet.setSheetSelectionBarVisible(false);
    sampleRegistrationSpreadsheet.setFunctionBarVisible(false);
  }

  public void generateSampleRegistrationSheet(MetadataType metaDataType) {
    sampleRegistrationSpreadsheet.addSheetToSpreadsheet(metaDataType);
  }

  public void resetLayout() {
    sampleInformationLayoutHandler.reset();
    experiment = null;
  }

  public void reloadSpreadsheet() {
    sampleRegistrationSpreadsheet.reloadSpreadsheet();
  }

  public void setBatchName(String text) {
    batchName.setText(text);
  }

  public boolean isInputValid() {
    return sampleInformationLayoutHandler.isInputValid();
  }

  public void setExperiment(Experiment experiment) {
    this.experiment = experiment.experimentId();
    experimentName.setText(experiment.getName());
    sampleRegistrationSpreadsheet.setExperimentMetadata(experiment);
  }

  public List<SampleRegistrationContent> getContent() {
    return sampleInformationLayoutHandler.getContent();
  }

  public ExperimentId getExperiment() {
    return experiment;
  }

  public void prefillConditionsAndReplicates(boolean isPrefilled) {
    sampleRegistrationSpreadsheet.prefillConditionsAndReplicates(isPrefilled);
  }

  public boolean isPrefilled() {
    return isPrefilled;
  }

  public void setPrefilled(boolean prefilled) {
    isPrefilled = prefilled;
  }

  private class SampleInformationLayoutHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = 2837608401189525502L;

    private void reset() {
      resetChildValues();
    }

    private void resetChildValues() {
      resetInstructions();
      resetSpreadSheet();
      hideErrorInstructions();
    }

    private void resetInstructions() {
      batchName.setText("");
      experimentName.setText("");
    }

    private void resetSpreadSheet() {
      sampleRegistrationSpreadsheet.reset();
    }

    private boolean isInputValid() {
      Result<Void, InvalidSpreadsheetInput> content = sampleRegistrationSpreadsheet.areInputsValid();
      if (content.isValue()) {
        hideErrorInstructions();
      }
      return content.onError(error -> displayErrorInstructions(error.getInvalidationReason()))
          .isValue();
    }

    private void displayErrorInstructions(String instructions) {
      errorInstructionSpan.removeAll();
      Span errorSpan = new Span();
      errorSpan.add(instructions);
      errorSpan.addClassName("error-text");
      errorInstructionSpan.add(errorSpan);
    }

    private void hideErrorInstructions() {
      errorInstructionSpan.removeAll();
    }

    private List<SampleRegistrationContent> getContent() {
      List<NGSRowDTO> filledRows = sampleRegistrationSpreadsheet.getFilledRows();
      List<SampleRegistrationContent> samplesToRegister = new ArrayList<>();
      filledRows.forEach(row -> {
        SampleRegistrationContent sampleRegistrationContent = new SampleRegistrationContent(
            row.sampleLabel(), row.bioReplicateID(), row.experimentalGroupId(), row.species(),
            row.specimen(),
            row.analyte(), row.analysisType(), row.customerComment());
        samplesToRegister.add(sampleRegistrationContent);
      });
      return samplesToRegister;
    }
  }
}
