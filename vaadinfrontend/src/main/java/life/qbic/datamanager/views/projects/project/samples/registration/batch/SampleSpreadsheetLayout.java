package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationSpreadsheet.NGSRowDTO;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;

/**
 * <b>Sample Spreadsheet Layout</b>
 * <p>
 * Layout which is responsible for hosting the spreadsheet in which the metadata information
 * associated for each sample will be provided>
 * </p>
 */
class SampleSpreadsheetLayout extends VerticalLayout {

  private final Span sampleInformationHeader = new Span("Sample Information");
  private final Span batchRegistrationInstruction = new Span();
  private final Label batchName = new Label();
  public final SampleRegistrationSpreadsheet sampleRegistrationSpreadsheet = new SampleRegistrationSpreadsheet();
  public final Button cancelButton = new Button("Cancel");
  public final Button addRowButton = new Button("Add Row");
  public final Button registerButton = new Button("Register");
  private final SampleInformationLayoutHandler sampleInformationLayoutHandler;

  SampleSpreadsheetLayout() {
    initContent();
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
    batchRegistrationInstruction.add("Please register your samples for Batch: ");
    batchRegistrationInstruction.add(batchName);
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
    this.setAlignSelf(Alignment.END, sampleInformationButtons);
    add(sampleInformationButtons);
  }

  private void styleSampleRegistrationSpreadSheet() {
    sampleRegistrationSpreadsheet.setSizeFull();
    sampleRegistrationSpreadsheet.setSheetSelectionBarVisible(true);
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

  public void setActiveExperiment(Experiment experiment) {
    SampleRegistrationSpreadsheet.setExperimentMetadata(experiment);
  }

  public List<SampleRegistrationContent> getContent() {
    return sampleInformationLayoutHandler.getContent();
  }

  private class SampleInformationLayoutHandler implements Serializable {

    @Serial
    private static final long serialVersionUID = 2837608401189525502L;
    private final List<Binder<?>> binders = new ArrayList<>();

    private void reset() {
      resetChildValues();
    }

    private void resetChildValues() {
      batchName.setText("");
      sampleRegistrationSpreadsheet.reset();
      sampleRegistrationSpreadsheet.reload();
    }

    private boolean isInputValid() {
      binders.forEach(Binder::validate);
      return binders.stream().allMatch(Binder::isValid);
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
