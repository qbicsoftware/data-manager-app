package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class BatchRegistrationDialog2 extends DialogWindow {

  private final Text userHelpText = new Text("Please register your samples.");
  private final Spreadsheet<SampleInfo> spreadsheet;

  public BatchRegistrationDialog2() {
    addClassName("batch-registration-dialog");
    setConfirmButtonLabel("Register");

    spreadsheet = new Spreadsheet<>();

    List<AnalysisMethod> sortedAnalysisMethods = Arrays.stream(AnalysisMethod.values())
        .sorted(Comparator.naturalOrder())
        .toList();
    spreadsheet.addColumn("Analysis to be performed",
            sampleInfo -> isNull(sampleInfo.getAnalysisToBePerformed()) ? null
                : sampleInfo.getAnalysisToBePerformed().label(),
            (sampleInfo, analysisToBePerformed) -> sampleInfo.setAnalysisToBePerformed(
                AnalysisMethod.forLabel(analysisToBePerformed)))
        .selectFrom(sortedAnalysisMethods,
            AnalysisMethod::label,
            getAnalysisMethodItemRenderer())
        .setRequired();

    spreadsheet.addColumn("Sample label", SampleInfo::getSampleLabel,
        SampleInfo::setSampleLabel);
    spreadsheet.addColumn("Biological replicate ID", SampleInfo::getBioReplicateId,
        SampleInfo::setBioReplicateId);
    spreadsheet.addColumn("Condition", SampleInfo::getCondition,
        SampleInfo::setCondition);
    spreadsheet.addColumn("Species", SampleInfo::getSpecies,
        SampleInfo::setSpecies);
    spreadsheet.addColumn("Specimen", SampleInfo::getSpecimen,
        SampleInfo::setSpecimen);
    spreadsheet.addColumn("Analyte", SampleInfo::getAnalyte,
        SampleInfo::setAnalyte);
    spreadsheet.addColumn("Customer comment", SampleInfo::getCustomerComment,
        SampleInfo::setCustomerComment);

    TextField batchNameField = new TextField();
    batchNameField.addClassName("batch-name-field");
    batchNameField.addValueChangeListener(
        this::onBatchNameChanged);

    Button prefillSpreadsheet = new Button();
    prefillSpreadsheet.setText("Prefill Spreadsheet");
    prefillSpreadsheet.setAriaLabel("Prefill complete sample batch");
    prefillSpreadsheet.addClickListener(this::onPrefillClicked);
    prefillSpreadsheet.addClassName("prefill-batch");

    Button addRow = new Button();
    addRow.setText("Add Row");
    addRow.addClickListener(this::onAddRowClicked);
    addRow.addClassName("add-batch-row");

    Button removeLastRow = new Button();
    removeLastRow.setText("Remove Row");
    removeLastRow.addClickListener(this::onRemoveLastRowClicked);
    removeLastRow.addClassName("remove-batch-row");


    // Register Batch
    //-------------------------
    // batchName -> prefillButton
    // please register your samples
    // ->     delete row -> add row
    // spreadsheet
    //-------------------------
    // -> cancel -> register
    setHeaderTitle("Register Batch");
    setResizable(true);

    Div batchControls = new Div();
    batchControls.addClassName("batch-controls");
    batchControls.add(batchNameField, prefillSpreadsheet);
    Div spreadsheetControls = new Div();
    spreadsheetControls.addClassName("spreadsheet-controls");
    spreadsheetControls.add(addRow, removeLastRow);

    add(batchControls,
        userHelpText,
        spreadsheetControls,
        spreadsheet);
  }

  private static ComponentRenderer<Span, AnalysisMethod> getAnalysisMethodItemRenderer() {
    return new ComponentRenderer<>(analysisMethod -> {
      var listItem = new Span();
      listItem.addClassName("spreadsheet-list-item");
      Span label = new Span(analysisMethod.label());
      label.setText(analysisMethod.label());
      var questionMarkIcon = VaadinIcon.QUESTION_CIRCLE_O.create();
      questionMarkIcon.setTooltipText(analysisMethod.description());
      listItem.add(label, questionMarkIcon);
      return listItem;
    });
  }

  private void onBatchNameChanged(ComponentValueChangeEvent<TextField, String> batchName) {
    this.userHelpText.setText(
        "Please register your samples for batch " + batchName.getValue() + ".");
  }

  private void onRemoveLastRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.removeLastRow();
    //TODO remove the last row from the spreadsheet
  }

  private void onAddRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.addRow(new SampleInfo());
    //TODO add a row to the spreadsheet
  }

  private void onPrefillClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.resetRows();
    //TODO prefill spreadsheet
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    System.out.println("spreadsheet.getData() = " + spreadsheet.getData());
    //TODO
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
  }

  @Override
  public void close() {
    super.close();
    reset();
  }

  protected void reset() {
    //TODO implement
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends ComponentEvent<BatchRegistrationDialog2> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(BatchRegistrationDialog2 source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  private static class SampleInfo {

    AnalysisMethod analysisToBePerformed;
    String sampleLabel;
    String bioReplicateId;
    String condition;
    String species;
    String specimen;
    String analyte;
    String customerComment;

    public AnalysisMethod getAnalysisToBePerformed() {
      return analysisToBePerformed;
    }

    public void setAnalysisToBePerformed(AnalysisMethod analysisToBePerformed) {
      this.analysisToBePerformed = analysisToBePerformed;
    }

    public String getSampleLabel() {
      return sampleLabel;
    }

    public void setSampleLabel(String sampleLabel) {
      this.sampleLabel = sampleLabel;
    }

    public String getBioReplicateId() {
      return bioReplicateId;
    }

    public void setBioReplicateId(String bioReplicateId) {
      this.bioReplicateId = bioReplicateId;
    }

    public String getCondition() {
      return condition;
    }

    public void setCondition(String condition) {
      this.condition = condition;
    }

    public String getSpecies() {
      return species;
    }

    public void setSpecies(String species) {
      this.species = species;
    }

    public String getSpecimen() {
      return specimen;
    }

    public void setSpecimen(String specimen) {
      this.specimen = specimen;
    }

    public String getAnalyte() {
      return analyte;
    }

    public void setAnalyte(String analyte) {
      this.analyte = analyte;
    }

    public String getCustomerComment() {
      return customerComment;
    }

    public void setCustomerComment(String customerComment) {
      this.customerComment = customerComment;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", SampleInfo.class.getSimpleName() + "[", "]")
          .add("analysisToBePerformed=" + analysisToBePerformed)
          .add("sampleLabel='" + sampleLabel + "'")
          .add("bioReplicateId='" + bioReplicateId + "'")
          .add("condition='" + condition + "'")
          .add("species='" + species + "'")
          .add("specimen='" + specimen + "'")
          .add("analyte='" + analyte + "'")
          .add("customerComment='" + customerComment + "'")
          .toString();
    }
  }

}
