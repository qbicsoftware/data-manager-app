package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.CancelConfirmationNotificationDialog;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.EditBatchDialog.ConfirmEvent.Data;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * A dialog used for editing sample and batch information within an experiment
 */
public class EditBatchDialog extends DialogWindow {

  private final SampleBatchInformationSpreadsheet spreadsheet;
  private final BatchId batchId;
  private final List<SampleInfo> existingSamples;
  private final TextField batchNameField;
  private final Span batchNameText;

  private final Span errorText = new Span("Unspecific Error message");
  private final SampleDeletionChecker deletionChecker;

  public EditBatchDialog(String experimentName,
      List<OntologyTerm> species,
      List<OntologyTerm> specimen,
      List<OntologyTerm> analytes,
      List<ExperimentalGroup> experimentalGroups,
      BatchId batchId,
      String batchName,
      List<SampleInfo> existingSamples,
      SampleDeletionChecker deletionCheck) {

    addClassName("batch-update-dialog");
    setConfirmButtonLabel("Save");

    deletionChecker = requireNonNull(deletionCheck, "deletionCheck must not be null");

    this.batchId = batchId;

    this.existingSamples = existingSamples.stream().map(SampleInfo::copy).toList();

    initCancelShortcuts(this::onCanceled);

    spreadsheet = new SampleBatchInformationSpreadsheet(experimentalGroups, species, specimen,
        analytes, true);

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

    setHeaderTitle("Edit sample batch");
    setResizable(true);

    Div batchControls = new Div();
    batchControls.addClassName("batch-controls");
    batchControls.add(batchNameField);

    Span userHelp1 = new Span("You are editing your samples for batch");
    Span experimentNameText = new Span(experimentName);
    experimentNameText.addClassName("experiment-name");
    Span userHelp2 = new Span("and experiment");
    batchNameText = new Span(batchName);
    batchNameText.addClassName("batch-name");
    Span userHelpText = new Span(userHelp1, batchNameText, userHelp2, experimentNameText);
    userHelpText.addClassName("user-help-text");

    Div spreadsheetHeader = new Div();
    spreadsheetHeader.addClassName("spreadsheet-header");

    Span spreadsheetControls = new Span();
    spreadsheetControls.addClassName("spreadsheet-controls");
    spreadsheetControls.add(addRow, removeLastRow);

    spreadsheetHeader.add(userHelpText, spreadsheetControls);
    spreadsheetHeader.addClassName("spreadsheet-header");

    errorText.addClassName("error-text");
    errorText.setVisible(false);

    Div spreadsheetContainer = new Div();

    spreadsheetContainer.addClassName("spreadsheet-container");
    spreadsheetContainer.add(spreadsheetHeader, errorText, spreadsheet);

    add(batchControls,
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

    spreadsheet.resetRows();
    // don't use field, but parameter in order to compare edits later
    for (SampleInfo existingSample : existingSamples) {
      spreadsheet.addRow(existingSample);
    }
  }

  private TextField createBatchNameField() {
    final TextField textField;
    textField = new TextField();
    textField.setLabel("Batch Name");
    textField.setPlaceholder("Please enter a name for this batch");
    textField.setRequired(true);
    // must contain at least one non-whitespace character and no leading/tailing whitespace.
    textField.setPattern("^\\S+(.*\\S)*$");
    textField.setErrorMessage(
        "The batch name must not be empty. It must not start nor end with whitespace.");
    return textField;
  }

  private void onBatchNameChanged(
      ComponentValueChangeEvent<TextField, String> batchNameChangedEvent) {
    batchNameText.setText(batchNameChangedEvent.getValue());
  }

  private void onRemoveLastRowClicked(ClickEvent<Button> clickEvent) {
    List<SampleInfo> tableData = spreadsheet.getData();
    Optional<SampleInfo> optionalSampleInfo = tableData.stream()
        .skip(tableData.size() - 1L)
        .findAny();
    optionalSampleInfo.ifPresent(sampleInfo -> {
      if (isNull(sampleInfo.getSampleId())) {
        spreadsheet.removeLastRow();
        return;
      }
      int dataRowIndex = spreadsheet.getDataRowIndex(sampleInfo);
      if (deletionChecker.canDeleteSample(sampleInfo.getSampleId())) {
        spreadsheet.removeLastRow();
      } else {
        displayRemoveRowError(dataRowIndex);
      }
    });
  }

  public void displayRemoveRowError(int dataRowIndex) {
    errorText.setText(
        "Sample #" + (dataRowIndex + 1) + " can not be removed because " + "data is attached");
    errorText.setVisible(true);
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
        .filter(it -> Objects.isNull(it.getSampleId()))
        .toList();
  }

  private static List<SampleInfo> extractDeletedSamples(final List<SampleInfo> originalSampleInfos,
      final List<SampleInfo> sampleInfos) {
    return originalSampleInfos.stream()
        .filter(originalSampleInfo -> sampleInfos.stream()
            .filter(sampleInfo -> nonNull(sampleInfo.getSampleId()))
            .noneMatch(it -> it.getSampleId().equals(originalSampleInfo.getSampleId())))
        .toList();
  }

  private static List<SampleInfo> extractChangedSamples(final List<SampleInfo> originalSampleInfos,
      final List<SampleInfo> sampleInfos) {
    return sampleInfos.stream()
        .filter(sampleInfo -> nonNull(sampleInfo.getSampleId()))
        .filter(sampleInfo -> originalSampleInfos.stream()
            .anyMatch(
                it -> it.getSampleId().equals(sampleInfo.getSampleId())
                    && !it.equals(sampleInfo)))
        .toList();
  }

  private void onCanceled() {
    CancelConfirmationNotificationDialog cancelDialog = new CancelConfirmationNotificationDialog()
        .withBodyText("You will lose any changes you made.")
        .withConfirmText("Discard changes")
        .withTitle("Discard batch changes?");
    cancelDialog.open();
    cancelDialog.addConfirmListener(event -> {
      cancelDialog.close();
      fireEvent(new CancelEvent(this, true));
    });
    cancelDialog.addCancelListener(
        event -> cancelDialog.close());
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    onCanceled();
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

  @FunctionalInterface

  public interface SampleDeletionChecker {

    boolean canDeleteSample(SampleId sampleId);

  }

}
