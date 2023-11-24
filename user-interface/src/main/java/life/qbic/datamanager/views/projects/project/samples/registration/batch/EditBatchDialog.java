package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditBatchDialog.ConfirmEvent.Data;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
/**
 * A dialog used for editing sample and batch information within an experiment
 */
public class EditBatchDialog extends DialogWindow {

  private final SampleBatchInformationSpreadsheet spreadsheet;
  private final BatchId batchId;
  private final List<SampleInfo> existingSamples;

  private final TextField batchNameField;
  private final Span batchNameText;

  public EditBatchDialog(String experimentName,
      List<Species> species,
      List<Specimen> specimen,
      List<Analyte> analytes,
      List<ExperimentalGroup> experimentalGroups,
      BatchId batchId,
      String batchName,
      List<SampleInfo> existingSamples) {

    addClassName("batch-update-dialog");
    setConfirmButtonLabel("Update Samples");

    this.batchId = batchId;
    this.existingSamples = existingSamples.stream().map(SampleInfo::clone).toList();

    spreadsheet = new SampleBatchInformationSpreadsheet(experimentalGroups, species, specimen,
        analytes, false);

    batchNameField = createBatchNameField();
    batchNameField.addClassName("batch-name-field");
    batchNameField.setValue(batchName);
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

    Span userHelp1 = new Span("You are editing samples for batch");
    Span experimentNameText = new Span(experimentName);
    experimentNameText.addClassName("experiment-name");
    Span userHelp2 = new Span("and experiment");
    batchNameText = new Span(batchName);
    batchNameText.addClassName("batch-name");
    Div userHelpText = new Div(userHelp1, batchNameText, userHelp2, experimentNameText);
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

    spreadsheet.resetRows();
    for (SampleInfo existingSample : existingSamples) {
      spreadsheet.addRow(existingSample);
    }
  }

  private TextField createBatchNameField() {
    final TextField batchNameField;
    batchNameField = new TextField();
    batchNameField.setLabel("Batch Name");
    batchNameField.setPlaceholder("Please enter a name for this batch");
    batchNameField.setRequired(true);
    // must contain at least one non-whitespace character and no leading/tailing whitespace.
    batchNameField.setPattern("^\\S+(.*\\S)*$");
    batchNameField.setErrorMessage(
        "The batch name must not be empty. It must not start nor end with whitespace.");
    return batchNameField;
  }

  private void onBatchNameChanged(
      ComponentValueChangeEvent<TextField, String> batchNameChangedEvent) {
    batchNameText.setText(batchNameChangedEvent.getValue());
  }

  private void onRemoveLastRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.removeLastRow();
  }

  private void onAddRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.addEmptyRow();
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
    List<SampleInfo> sampleInfos = spreadsheet.getData();
    fireEvent(new ConfirmEvent(this, clickEvent.isFromClient(),
        new Data(batchId,
            batchNameField.getValue(),
            extractAddedSamples(sampleInfos),
            extractChangedSamples(existingSamples, sampleInfos),
            extractDeletedSamples(existingSamples, sampleInfos))));
  }

  private static List<SampleInfo> extractAddedSamples(final List<SampleInfo> sampleInfos) {
    return sampleInfos.stream()
        .filter(it -> it.getSampleCode().isEmpty())
        .toList();
  }

  private static List<SampleInfo> extractDeletedSamples(final List<SampleInfo> originalSampleInfos,
      final List<SampleInfo> sampleInfos) {
    return originalSampleInfos.stream()
        .filter(originalSampleInfo -> sampleInfos.stream()
            .noneMatch(it -> it.getSampleId().equals(originalSampleInfo.getSampleId())))
        .toList();
  }

  private static List<SampleInfo> extractChangedSamples(final List<SampleInfo> originalSampleInfos,
      final List<SampleInfo> sampleInfos) {
    return sampleInfos.stream()
        .filter(sampleInfo -> originalSampleInfos.stream()
            .anyMatch(
                it -> it.getSampleId().equals(sampleInfo.getSampleId())
                    && !it.equals(sampleInfo)))
        .toList();
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  /**
   * Adds a listener for user cancellation.
   *
   * @param listener the listener to add
   */
  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  /**
   * Adds a listener for user confirmation
   * @param listener the listener to add
   */
  public void addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    addListener(ConfirmEvent.class, listener);
  }

  public static class CancelEvent extends ComponentEvent<EditBatchDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(EditBatchDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class ConfirmEvent extends ComponentEvent<EditBatchDialog> {

    /**
     * The data the user confirmed
     * @param batchId the batch id
     * @param batchName the batch name
     * @param addedSamples the information of samples added
     * @param changedSamples the information of samples changed
     * @param removedSamples the information of samples removed
     */
    public record Data(BatchId batchId, String batchName, List<SampleInfo> addedSamples,
                       List<SampleInfo> changedSamples, List<SampleInfo> removedSamples) {


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
    public ConfirmEvent(EditBatchDialog source, boolean fromClient, Data data) {
      super(source, fromClient);
      this.data = data;
    }

    public Data getData() {
      return data;
    }
  }

}
