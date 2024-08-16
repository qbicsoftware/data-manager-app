package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.ValidationMode;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent.Data;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;

/**
 * A dialog used for sample batch registration.
 */
public class BatchRegistrationDialog extends DialogWindow {

  private final TextField batchNameField;

  private final List<ExperimentalGroup> experimentalGroups;
  private final List<OntologyTerm> species;
  private final List<OntologyTerm> specimen;
  private final List<OntologyTerm> analytes;
  private final SampleBatchInformationSpreadsheet spreadsheet;

  public BatchRegistrationDialog(String experimentName,
      List<OntologyTerm> species,
      List<OntologyTerm> specimen,
      List<OntologyTerm> analytes,
      List<ExperimentalGroup> experimentalGroups) {

    addClassName("batch-registration-dialog");
    setConfirmButtonLabel("Register");

    this.experimentalGroups = new ArrayList<>(experimentalGroups);
    this.species = new ArrayList<>(species);
    this.specimen = new ArrayList<>(specimen);
    this.analytes = new ArrayList<>(analytes);
    this.spreadsheet = new SampleBatchInformationSpreadsheet(experimentalGroups, this.species,
        this.specimen,
        this.analytes, false);

    batchNameField = new TextField();
    batchNameField.addClassName("batch-name-field");
    batchNameField.setLabel("Batch Name");
    batchNameField.setPlaceholder("Please enter a name for your batch");
    batchNameField.setRequired(true);
    // must contain at least one non-whitespace character and no leading/tailing whitespace.
    batchNameField.setPattern("^\\S+(.*\\S)*$");
    batchNameField.setErrorMessage(
        "The batch name must not be empty. It must not start nor end with whitespace.");
    batchNameField.addValueChangeListener(this::onBatchNameChanged);

    Div prefillSection = new Div();
    Button prefillSpreadsheet = new Button();
    prefillSpreadsheet.setText("Prefill spreadsheet");
    prefillSpreadsheet.setAriaLabel("Prefill spreadsheet");
    prefillSpreadsheet.addClickListener(this::onPrefillClicked);
    prefillSpreadsheet.addClassName("prefill-batch");

    Span prefillText = new Span(
        "Do you want to register a complete batch (all possible permutations of conditions and replicates) with some prefilled information?");
    prefillSection.add(prefillText, prefillSpreadsheet);
    prefillSection.addClassName("prefill-section");

    Button addRow = new Button();
    addRow.setText("Add Row");
    addRow.addClickListener(this::onAddRowClicked);
    addRow.addClassName("add-batch-row");

    Button removeLastRow = new Button();
    removeLastRow.setText("Remove Row");
    removeLastRow.addClickListener(this::onRemoveLastRowClicked);
    removeLastRow.addClassName("remove-batch-row");

    setHeaderTitle("Register sample batch");
    setResizable(true);

    Div batchControls = new Div();
    batchControls.addClassName("batch-controls");
    batchControls.add(batchNameField);

    Div spreadsheetContainer = new Div();

    Span pleaseRegisterText = new Span("Please register your samples for experiment: ");
    Span experimentNameText = new Span(experimentName);
    experimentNameText.setClassName("experiment-name");
    Span userHelpText = new Span(pleaseRegisterText, experimentNameText);
    userHelpText.addClassName("user-help-text");

    Span spreadsheetControls = new Span();
    spreadsheetControls.addClassName("spreadsheet-controls");
    spreadsheetControls.add(addRow, removeLastRow);

    Span spreadsheetHeader = new Span(userHelpText, spreadsheetControls);
    spreadsheetHeader.addClassName("spreadsheet-header");

    Span errorText = new Span("Unspecific Error message");
    errorText.addClassName("error-text");
    errorText.setVisible(false);

    spreadsheetContainer.add(spreadsheetHeader, errorText, spreadsheet);
    spreadsheetContainer.addClassName("spreadsheet-container");

    add(batchControls,
        prefillSection,
        spreadsheetContainer);

    batchNameField.focus();

    spreadsheet.addValidationChangeListener(
        validationChangeEvent -> {
          if (validationChangeEvent.isInvalid()) {
            errorText.setText(validationChangeEvent.getSource().getErrorMessage());
            errorText.setVisible(true);
          } else {
            errorText.setVisible(false);
          }
        });
  }

  private static List<SampleInfo> prefilledSampleInfos(List<OntologyTerm> species,
      List<OntologyTerm> specimen, List<OntologyTerm> analytes,
      List<ExperimentalGroup> experimentalGroups) {

    List<SampleInfo> sampleInfos = new ArrayList<>();
    for (ExperimentalGroup experimentalGroup : experimentalGroups) {
      for (int i = 0; i < experimentalGroup.sampleSize(); i++) {
        SampleInfo sampleInfo = new SampleInfo();
        sampleInfo.setExperimentalGroup(experimentalGroup);
        if (species.size() == 1) {
          sampleInfo.setSpecies(species.get(0));
        }
        if (specimen.size() == 1) {
          sampleInfo.setSpecimen(specimen.get(0));
        }
        if (analytes.size() == 1) {
          sampleInfo.setAnalyte(analytes.get(0));
        }
        sampleInfos.add(sampleInfo);
      }
    }
    return sampleInfos;
  }

  private List<SampleInfo> prefilledSampleInfos() {
    return prefilledSampleInfos(species, specimen, analytes, experimentalGroups);
  }

  private void onBatchNameChanged(
      ComponentValueChangeEvent<TextField, String> batchNameChangedEvent) {
    /* do nothing */
  }

  private void onRemoveLastRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.removeLastRow();
  }

  private void onAddRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.addEmptyRow();
  }

  private void onPrefillClicked(ClickEvent<Button> clickEvent) {
    ValidationMode validationMode = spreadsheet.getValidationMode();
    spreadsheet.setValidationMode(ValidationMode.LAZY);
    spreadsheet.resetRows();
    for (SampleInfo sampleInfo : prefilledSampleInfos()) {
      spreadsheet.addRow(sampleInfo);
    }
    spreadsheet.setValidationMode(validationMode);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.validate();
    if (spreadsheet.isInvalid()) {
      return;
    }
    if (batchNameField.isInvalid()) {
      return;
    }
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(),
        new Data(batchNameField.getValue(), spreadsheet.getData())));
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    close();
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public void addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
  }

  public static class CancelEvent extends ComponentEvent<BatchRegistrationDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(BatchRegistrationDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ConfirmEvent extends ComponentEvent<BatchRegistrationDialog> {

    private final Data data;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(BatchRegistrationDialog source, boolean fromClient, Data data) {
      super(source, fromClient);
      this.data = data;
    }

    public Data getData() {
      return data;
    }

    public record Data(String batchName, List<SampleInfo> samples) {

    }
  }

}
