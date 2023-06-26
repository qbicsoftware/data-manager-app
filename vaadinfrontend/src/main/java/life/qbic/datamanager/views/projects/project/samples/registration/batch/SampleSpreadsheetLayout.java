package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationSpreadsheet.InvalidSpreadsheetRow;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationSpreadsheet.NGSRowDTO;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

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
  private final Span batchName = new Span();
  private final Span experimentName = new Span();
  public final transient SampleRegistrationSpreadsheet sampleRegistrationSpreadsheet = new SampleRegistrationSpreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button addRowButton = new Button("Add Row");
  public final Button registerButton = new Button("Register");
  private final SampleInformationLayoutHandler sampleInformationLayoutHandler;

  SampleSpreadsheetLayout() {
    initContent();
    this.addClassName("sample-information");
    this.setSizeFull();
    sampleInformationLayoutHandler = new SampleInformationLayoutHandler();
  }

  private void initContent() {
    initHeaderAndInstruction();
    add(sampleRegistrationSpreadsheet);
    styleSampleRegistrationSpreadSheet();
    initButtonLayout();
  }

  private void initHeaderAndInstruction() {
    sampleInformationHeader.addClassNames("text-xl", "font-bold", "text-secondary");
    Span instructionSpan = new Span();
    instructionSpan.add("Please register your samples for experiment: ");
    instructionSpan.add(experimentName);
    experimentName.addClassNames(FontWeight.BOLD, FontWeight.BLACK);
    instructionSpan.add(" in batch: ");
    instructionSpan.add(batchName);
    batchRegistrationInstruction.add(instructionSpan);
    batchName.addClassNames(FontWeight.BOLD, FontWeight.BLACK);
    add(sampleInformationHeader);
    add(batchRegistrationInstruction);
  }

  private void initButtonLayout() {
    HorizontalLayout sampleInformationButtons = new HorizontalLayout();
    addRowButton.addClickListener(
        (ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> sampleRegistrationSpreadsheet.addRow());
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    sampleInformationButtons.add(addRowButton, cancelButton, registerButton);
    add(sampleInformationButtons);
  }

  private void styleSampleRegistrationSpreadSheet() {
    sampleRegistrationSpreadsheet.setSizeFull();
    sampleRegistrationSpreadsheet.setSheetSelectionBarVisible(false);
    sampleRegistrationSpreadsheet.setFunctionBarVisible(false);
  }

  public void generateSampleRegistrationSheet(MetadataType metaDataType) {
    sampleRegistrationSpreadsheet.reset();
    sampleRegistrationSpreadsheet.addSheetToSpreadsheet(metaDataType);
    sampleRegistrationSpreadsheet.reload();
  }

  public void reset() {
    sampleInformationLayoutHandler.reset();
  }

  public void setBatchName(String text) {
    batchName.setText(text);
  }

  public boolean isInputValid() {
    return sampleInformationLayoutHandler.isInputValid();
  }

  public void setExperiment(Experiment experiment) {
    SampleRegistrationSpreadsheet.setExperimentMetadata(experiment);
    experimentName.setText(experiment.getName());
  }

  public List<SampleRegistrationContent> getContent() {
    return sampleInformationLayoutHandler.getContent();
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
    }

    private void resetInstructions() {
      batchName.setText("");
      experimentName.setText("");
    }

    private void resetSpreadSheet() {
      sampleRegistrationSpreadsheet.reset();
      sampleRegistrationSpreadsheet.reload();
    }

    private boolean isInputValid() {
      Result<Void, InvalidSpreadsheetRow> content = sampleRegistrationSpreadsheet.areInputsValid();
      return content.onError(error -> displayInputInvalidMessage(error.getInvalidationReason())).isValue();
    }

    private void displayInputInvalidMessage(String invalidationReason) {
      InformationMessage infoMessage = new InformationMessage(
          "Incomplete or erroneous metadata found",
          invalidationReason);
      StyledNotification notification = new StyledNotification(infoMessage);
      // we need to reload the sheet as the notification popup and removal destroys the spreadsheet UI for some reason...
      notification.addAttachListener(
          (ComponentEventListener<AttachEvent>) attachEvent -> sampleRegistrationSpreadsheet.reload());
      notification.addDetachListener(
          (ComponentEventListener<DetachEvent>) detachEvent -> sampleRegistrationSpreadsheet.reload());
      notification.open();
    }

    private List<SampleRegistrationContent> getContent() {
      List<NGSRowDTO> filledRows = sampleRegistrationSpreadsheet.getFilledRows();
      List<SampleRegistrationContent> samplesToRegister = new ArrayList<>();
      filledRows.forEach(row -> {
        SampleRegistrationContent sampleRegistrationContent = new SampleRegistrationContent(
            row.sampleLabel(), row.bioReplicateID(), row.experimentalGroupId(), row.species(),
            row.specimen(),
            row.analyte(), row.customerComment());
        samplesToRegister.add(sampleRegistrationContent);
      });
      return samplesToRegister;
    }
  }
}
