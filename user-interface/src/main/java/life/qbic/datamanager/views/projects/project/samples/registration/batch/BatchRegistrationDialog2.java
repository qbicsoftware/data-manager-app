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
import java.util.Optional;
import java.util.StringJoiner;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.ValidationMode;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog2.ConfirmEvent.Data;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
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

  private final Text userHelpText = new Text("");
  private final Spreadsheet<SampleInfo> spreadsheet;
  private final TextField batchNameField;

  public BatchRegistrationDialog2(List<Species> species, List<Specimen> specimen,
      List<Analyte> analytes) {

    addClassName("batch-registration-dialog");
    setConfirmButtonLabel("Register");

    spreadsheet = new Spreadsheet<>();

    List<AnalysisMethod> sortedAnalysisMethods = Arrays.stream(AnalysisMethod.values())
        .sorted(Comparator.comparing(AnalysisMethod::label))
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
            SampleInfo::setSampleLabel)
        .setRequired();

    spreadsheet.addColumn("Biological replicate ID", SampleInfo::getBioReplicateId,
            SampleInfo::setBioReplicateId)
        .setRequired();

    spreadsheet.addColumn("Condition", SampleInfo::getCondition,
            SampleInfo::setCondition)
        .setRequired();

    spreadsheet.addColumn("Species",
            sampleInfo -> Optional.ofNullable(sampleInfo.getSpecies())
                .map(Species::label)
                .orElse(null),
            SampleInfo::setSpecies)
        .selectFrom(species, Species::label)
        .setRequired();

    spreadsheet.addColumn("Specimen",
            sampleInfo -> Optional.ofNullable(sampleInfo.getSpecimen())
                .map(Specimen::label)
                .orElse(null),
            SampleInfo::setSpecimen)
        .selectFrom(specimen, Specimen::label)
        .setRequired();

    spreadsheet.addColumn("Analyte",
            sampleInfo -> Optional.ofNullable(sampleInfo.getAnalyte())
                .map(Analyte::label)
                .orElse(null),
            SampleInfo::setAnalyte)
        .selectFrom(analytes, Analyte::label)
        .setRequired();

    spreadsheet.addColumn("Customer comment", SampleInfo::getCustomerComment,
        SampleInfo::setCustomerComment);

    spreadsheet.setValidationMode(ValidationMode.EAGER);

    batchNameField = new TextField();
    batchNameField.addClassName("batch-name-field");
    batchNameField.addValueChangeListener(
        this::onBatchNameChanged);
    batchNameField.setLabel("Batch Name");
    batchNameField.setRequired(true);
    batchNameField.setPattern(".*\\S+.*"); // must contain at least one non-whitespace character

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

    setHeaderTitle("Register Batch");
    setResizable(true);

    Div batchControls = new Div();
    batchControls.addClassName("batch-controls");
    batchControls.add(batchNameField);

    Div spreadsheetControls = new Div();
    spreadsheetControls.addClassName("spreadsheet-controls");

    Span rowControls = new Span();
    rowControls.addClassName("row-controls");
    rowControls.add(addRow, removeLastRow);

    Span errorText = new Span("Unspecific Error message");
    errorText.addClassName("error-text");
    errorText.setVisible(false);

    spreadsheetControls.add(prefillSpreadsheet, errorText, rowControls);

    add(batchControls,
        userHelpText,
        spreadsheetControls,
        spreadsheet);

    updateUserHelpText(batchNameField.getValue());
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

  private void onBatchNameChanged(
      ComponentValueChangeEvent<TextField, String> batchNameChangedEvent) {
    updateUserHelpText(batchNameChangedEvent.getValue());
  }

  private void updateUserHelpText(String batchName) {
    String text = batchName.isBlank()
        ? "Please name your batch."
        : "Please register your samples for batch '" + batchName + "'.";
    this.userHelpText.setText(text);
  }

  private void onRemoveLastRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.removeLastRow();
  }

  private void onAddRowClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.setValidationMode(ValidationMode.LAZY);
    spreadsheet.addRow(new SampleInfo());
    spreadsheet.setValidationMode(ValidationMode.EAGER);
  }

  private void onPrefillClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.setValidationMode(ValidationMode.LAZY);
    spreadsheet.resetRows();
    for (SampleInfo sampleInfo : List.of(new SampleInfo(),
        new SampleInfo())) { //FIXME replace with actual prefilled model
      spreadsheet.addRow(sampleInfo);
    }
    spreadsheet.setValidationMode(ValidationMode.EAGER);
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

  public static class ConfirmEvent extends ComponentEvent<BatchRegistrationDialog2> {

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
    public ConfirmEvent(BatchRegistrationDialog2 source, boolean fromClient, Data data) {
      super(source, fromClient);
      this.data = data;
    }

    public Data getData() {
      return data;
    }
  }

  public static class SampleInfo {

    private AnalysisMethod analysisToBePerformed;
    private String sampleLabel;
    private String bioReplicateId;
    private String condition;
    private Species species;
    private Specimen specimen;
    private Analyte analyte;
    private String customerComment;

    public static SampleInfo create(AnalysisMethod analysisMethod,
        String sampleLabel,
        String bioReplicateId,
        String condition,
        Species species,
        Specimen specimen,
        Analyte analyte,
        String customerComment) {
      SampleInfo sampleInfo = new SampleInfo();
      sampleInfo.setAnalysisToBePerformed(analysisMethod);
      sampleInfo.setSampleLabel(sampleLabel);
      sampleInfo.setBioReplicateId(bioReplicateId);
      sampleInfo.setCondition(condition);
      sampleInfo.setSpecies(species);
      sampleInfo.setSpecimen(specimen);
      sampleInfo.setAnalyte(analyte);
      sampleInfo.setCustomerComment(customerComment);
      return sampleInfo;
    }

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

    public Species getSpecies() {
      return species;
    }

    public void setSpecies(Species species) {
      this.species = species;
    }

    public void setSpecies(String label) {
      if (isNull(label)) {
        this.species = null;
        return;
      }
      if (label.isBlank()) {
        this.species = null;
        return;
      }
      this.species = Species.create(label);
    }

    public Specimen getSpecimen() {
      return specimen;
    }

    public void setSpecimen(Specimen specimen) {
      this.specimen = specimen;
    }

    public void setSpecimen(String label) {
      if (isNull(label)) {
        this.specimen = null;
        return;
      }
      if (label.isBlank()) {
        this.specimen = null;
        return;
      }
      this.specimen = Specimen.create(label);
    }

    public Analyte getAnalyte() {
      return analyte;
    }

    public void setAnalyte(Analyte analyte) {
      this.analyte = analyte;
    }

    public void setAnalyte(String label) {
      if (isNull(label)) {
        this.analyte = null;
        return;
      }
      if (label.isBlank()) {
        this.analyte = null;
        return;
      }
      this.analyte = Analyte.create(label);
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
