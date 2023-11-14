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
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.ValidationMode;
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

  private final Text userHelpText = new Text("Please register your samples.");
  private final Spreadsheet<SampleInfo> spreadsheet;

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
        SampleInfo::setCondition);
    spreadsheet.addColumn("Species", SampleInfo::getSpecies,
            SampleInfo::setSpecies)
        .selectFrom(species, Species::label)
        .setRequired();
    spreadsheet.addColumn("Specimen", SampleInfo::getSpecimen,
            SampleInfo::setSpecimen)
        .selectFrom(specimen, Specimen::label)
        .setRequired();
    spreadsheet.addColumn("Analyte", SampleInfo::getAnalyte,
            SampleInfo::setAnalyte)
        .selectFrom(analytes, Analyte::label)
        .setRequired();
    spreadsheet.addColumn("Customer comment", SampleInfo::getCustomerComment,
        SampleInfo::setCustomerComment);

    spreadsheet.setValidationMode(ValidationMode.EAGER);


    TextField batchNameField = new TextField();
    batchNameField.addClassName("batch-name-field");
    batchNameField.addValueChangeListener(
        this::onBatchNameChanged);
    batchNameField.setLabel("Batch Name");
    batchNameField.setRequired(true);

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

    spreadsheet.addValidationChangeListener(
        validationChangeEvent -> {
          if (validationChangeEvent.isInvalid()) {
            errorText.setText(validationChangeEvent.getSource().getErrorMessage());
            errorText.setVisible(true);
          } else {
            errorText.setVisible(false);
          }
        });

    spreadsheetControls.add(prefillSpreadsheet, errorText, rowControls);

    add(batchControls,
        userHelpText,
        spreadsheetControls,
        spreadsheet);
    batchNameField.focus();
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

  private List<SampleInfo> generatePrefilledSampleInformation() {
    //TODO replace with actual data
    return List.of(
        SampleInfo.create(AnalysisMethod.SIXTEEN_S, "sample 1", "bio-replicate 1", "bla condition",
            "some species", "some specimen", "some analyte", "no comment at this time"),
        SampleInfo.create(AnalysisMethod.SIXTEEN_S, "sample 2", "bio-replicate 2", "bla condition",
            "some species", "some specimen", "some analyte", "no comment at this time"),
        SampleInfo.create(AnalysisMethod.SIXTEEN_S, "sample 3", "bio-replicate 3", "bla condition",
            "some species", "some specimen", "some analyte", "no comment at this time"),
        SampleInfo.create(AnalysisMethod.SIXTEEN_S, "sample 4", "bio-replicate 4", "bla condition",
            "some species", "some specimen", "some analyte", "no comment at this time"),
        SampleInfo.create(AnalysisMethod.SIXTEEN_S, "sample 5", "bio-replicate 5", "bla condition",
            "some species", "some specimen", "some analyte", "no comment at this time"),
        SampleInfo.create(AnalysisMethod.SIXTEEN_S, "sample 6", "bio-replicate 6", "bla condition",
            "some species", "some specimen", "some analyte", "no comment at this time"),
        SampleInfo.create(AnalysisMethod.SIXTEEN_S, "sample 7", "bio-replicate 7", "bla condition",
            "some species", "some specimen", "some analyte", "no comment at this time")
    );
  }

  private void onBatchNameChanged(
      ComponentValueChangeEvent<TextField, String> batchNameChangedEvent) {
    String text = batchNameChangedEvent.getValue().isBlank()
        ? "Please name your batch."
        : "Please register your samples for batch '" + batchNameChangedEvent.getValue() + "'.";
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
    for (SampleInfo sampleInfo : generatePrefilledSampleInformation()) {
      spreadsheet.addRow(sampleInfo);
    }
    spreadsheet.setValidationMode(ValidationMode.EAGER);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    spreadsheet.validate();
    System.out.println("spreadsheet.getData() = " + spreadsheet.getData());
    //TODO
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
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

    private AnalysisMethod analysisToBePerformed;
    private String sampleLabel;
    private String bioReplicateId;
    private String condition;
    private String species;
    private String specimen;
    private String analyte;
    private String customerComment;

    public static SampleInfo create(AnalysisMethod analysisMethod,
        String sampleLabel,
        String bioReplicateId,
        String condition,
        String species,
        String specimen,
        String analyte,
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
