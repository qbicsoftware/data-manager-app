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
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.ValidationMode;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent.Data;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;

/**
 * A dialog used for sample batch registration.
 */
public class BatchUpdateDialog extends DialogWindow {

  private final SampleBatchInformationSpreadsheet spreadsheet;

  private final TextField batchNameField;

  private final List<ExperimentalGroup> experimentalGroups;
  private final List<Species> species;
  private final List<Specimen> specimen;
  private final List<Analyte> analytes;

  public BatchUpdateDialog(String experimentName,
      List<Species> species,
      List<Specimen> specimen,
      List<Analyte> analytes,
      List<ExperimentalGroup> experimentalGroups) {

    addClassName("batch-update-dialog");
    setConfirmButtonLabel("Update Samples");

    this.experimentalGroups = new ArrayList<>(experimentalGroups);
    this.species = new ArrayList<>(species);
    this.specimen = new ArrayList<>(specimen);
    this.analytes = new ArrayList<>(analytes);

    spreadsheet = new SampleBatchInformationSpreadsheet(experimentalGroups, species, specimen,
        analytes, false);

    batchNameField = new TextField();
    batchNameField.addClassName("batch-name-field");
    batchNameField.setLabel("Batch Name");
    batchNameField.setPlaceholder("Please enter a name for this batch");
    batchNameField.setRequired(true);
    // must contain at least one non-whitespace character and no leading/tailing whitespace.
    batchNameField.setPattern("^\\S+(.*\\S)*$");
    batchNameField.setErrorMessage(
        "The batch name must not be empty. It must not start nor end with whitespace.");
    batchNameField.addValueChangeListener(this::onBatchNameChanged);

    Button addRow = new Button();
    addRow.setText("Add Row");
    addRow.addClickListener(this::onAddRowClicked);
    addRow.addClassName("add-batch-row");

    Button removeLastRow = new Button();
    removeLastRow.setText("Remove Row");
    removeLastRow.addClickListener(this::onRemoveLastRowClicked);
    removeLastRow.addClassName("remove-batch-row");

    setHeaderTitle("Update Batch");
    setResizable(true);

    Div batchControls = new Div();
    batchControls.addClassName("batch-controls");
    batchControls.add(batchNameField);

    Span pleaseRegisterText = new Span("Please register your samples for experiment:");
    Span experimentNameText = new Span(experimentName);
    experimentNameText.setClassName("experiment-name");
    Div userHelpText = new Div(pleaseRegisterText, experimentNameText);
    userHelpText.addClassName("user-help-text");

    Div spreadsheetControls = new Div();
    spreadsheetControls.addClassName("spreadsheet-controls");

    Span rowControls = new Span();
    rowControls.addClassName("row-controls");
    rowControls.add(addRow, removeLastRow);

    Span errorText = new Span("Unspecific Error message");
    errorText.addClassName("error-text");
    errorText.setVisible(false);

    spreadsheetControls.add(rowControls, errorText);

    add(batchControls,
        userHelpText,
        spreadsheetControls,
        spreadsheet);

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

  private List<SampleInfo> prefilledSampleInfos() {
    return prefilledSampleInfos(species, specimen, analytes, experimentalGroups);
  }

  private static List<SampleInfo> prefilledSampleInfos(List<Species> species,
      List<Specimen> specimen, List<Analyte> analytes, List<ExperimentalGroup> experimentalGroups) {

    List<SampleInfo> sampleInfos = new ArrayList<>();
    for (ExperimentalGroup experimentalGroup : experimentalGroups) {
      List<BiologicalReplicate> sortedReplicates = experimentalGroup.biologicalReplicates().stream()
          .sorted(Comparator.comparing(BiologicalReplicate::label))
          .toList();
      for (BiologicalReplicate biologicalReplicate : sortedReplicates) {
        // new sampleInfo
        SampleInfo sampleInfo = new SampleInfo();
        sampleInfo.setBiologicalReplicate(biologicalReplicate);
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
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public void addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
  }

  public static class CancelEvent extends ComponentEvent<BatchUpdateDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(BatchUpdateDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ConfirmEvent extends ComponentEvent<BatchUpdateDialog> {

    public record Data(String batchName, List<SampleInfo> samples) {

    }

    private final Data data;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public ConfirmEvent(BatchUpdateDialog source, boolean fromClient, Data data) {
      super(source, fromClient);
      this.data = data;
    }

    public Data getData() {
      return data;
    }
  }

}
