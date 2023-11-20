package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.isNull;
import static java.util.function.Function.identity;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet.ValidationMode;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog2.ConfirmEvent.Data;
import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
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

  private final Spreadsheet<SampleInfo> spreadsheet;

  private final TextField batchNameField;

  private final List<ExperimentalGroup> experimentalGroups;
  private final List<Species> species;
  private final List<Specimen> specimen;
  private final List<Analyte> analytes;


  public BatchRegistrationDialog2(String experimentName,
      List<Species> species,
      List<Specimen> specimen,
      List<Analyte> analytes,
      List<ExperimentalGroup> experimentalGroups) {

    addClassName("batch-registration-dialog");
    setConfirmButtonLabel("Register");

    this.experimentalGroups = new ArrayList<>(experimentalGroups);

    List<AnalysisMethod> sortedAnalysisMethods = Arrays.stream(AnalysisMethod.values())
        .sorted(Comparator.comparing(AnalysisMethod::label))
        .toList();

    spreadsheet = new Spreadsheet<>();


    spreadsheet.addColumn("Analysis to be performed",
            SampleInfo::getAnalysisToBePerformed,
            AnalysisMethod::label,
            (sampleInfo, analysisToBePerformed) -> sampleInfo.setAnalysisToBePerformed(
                AnalysisMethod.forLabel(analysisToBePerformed)))
        .selectFrom(sortedAnalysisMethods,
            identity(),
            getAnalysisMethodItemRenderer())
        .setRequired();

    spreadsheet.addColumn("Sample label", SampleInfo::getSampleLabel,
            SampleInfo::setSampleLabel)
        .requireDistinctValues()
        .setRequired();

    spreadsheet.addColumn("Biological replicate ID",
            SampleInfo::getBiologicalReplicate,
            BiologicalReplicate::label,
            this::updateSampleInfoWithMatchingBiologicalReplicate)
        .selectFrom(sampleInfo -> Optional.ofNullable(sampleInfo.getExperimentalGroup())
            .map(ExperimentalGroup::biologicalReplicates).orElse(List.of()), identity())
        .setRequired();

    spreadsheet.addColumn("Condition", SampleInfo::getExperimentalGroup,
            experimentalGroup -> formatConditionString(experimentalGroup.condition()),
            (sampleInfo, conditionString) -> updateSampleInfoWithMatchingExperimentalGroup(
                experimentalGroups, sampleInfo, conditionString))
        .selectFrom(experimentalGroups, identity())
        .setRequired();

    this.species = species;
    spreadsheet.addColumn("Species",
            SampleInfo::getSpecies,
            Species::label,
            SampleInfo::setSpecies)
        .selectFrom(this.species, identity())
        .setRequired();

    this.specimen = specimen;
    spreadsheet.addColumn("Specimen",
            SampleInfo::getSpecimen,
            Specimen::label,
            SampleInfo::setSpecimen)
        .selectFrom(this.specimen, identity())
        .setRequired();

    this.analytes = analytes;
    spreadsheet.addColumn("Analyte",
            sampleInfo -> Optional.ofNullable(sampleInfo.getAnalyte())
                .map(Analyte::label)
                .orElse(null),
            SampleInfo::setAnalyte)
        .selectFrom(this.analytes, Analyte::label)
        .setRequired();

    spreadsheet.addColumn("Customer comment", SampleInfo::getCustomerComment,
        SampleInfo::setCustomerComment);

    spreadsheet.setValidationMode(ValidationMode.EAGER);

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

    Div prefillSection = new Div();
    Button prefillSpreadsheet = new Button();
    prefillSpreadsheet.setText("Prefill Spreadsheet");
    prefillSpreadsheet.setAriaLabel("Prefill complete sample batch");
    prefillSpreadsheet.addClickListener(this::onPrefillClicked);
    prefillSpreadsheet.addClassName("prefill-batch");

    Span prefillText = new Span(
        "Do you want to register a batch containing all biological replicates? You can prefill information already know to the system."
    );
    prefillSection.add(prefillText, prefillSpreadsheet);

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
        prefillSection,
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

  private void updateSampleInfoWithMatchingBiologicalReplicate(SampleInfo sampleInfo,
      String value) {

    Optional.ofNullable(sampleInfo.getExperimentalGroup())
        .ifPresentOrElse(
            experimentalGroup -> {
              BiologicalReplicate matchingBiologicalReplicate = sampleInfo.getExperimentalGroup()
                  .biologicalReplicates().stream()
                  .filter(biologicalReplicate -> biologicalReplicate.label().equals(value))
                  .findAny().orElse(null);
              sampleInfo.setBiologicalReplicate(matchingBiologicalReplicate);
            },
            /* It was requested that biological replicates can be entered even if they are not from the condition? */
            () -> {
              BiologicalReplicate matchingBiologicalReplicate = experimentalGroups.stream()
                  .flatMap(experimentalGroup -> experimentalGroup.biologicalReplicates().stream())
                  .filter(biologicalReplicate -> biologicalReplicate.label().equals(value))
                  .findAny().orElse(null);
              sampleInfo.setBiologicalReplicate(matchingBiologicalReplicate);
            }
        );
  }

  private static void updateSampleInfoWithMatchingExperimentalGroup(
      List<ExperimentalGroup> experimentalGroups, SampleInfo sampleInfo,
      String conditionString) {
    // find condition producing the same condition string
    Condition matchingCondition = experimentalGroups.stream()
        .map(ExperimentalGroup::condition)
        .filter(it -> formatConditionString(it).equals(conditionString))
        .findAny().orElse(null);
    // find experimentalGroup with the condition
    Optional<ExperimentalGroup> matchingExperimentalGroup = experimentalGroups.stream()
        .filter(
            experimentalGroup -> experimentalGroup.condition().equals(matchingCondition))
        .findAny();
    // set experimental group in sampleInfo
    matchingExperimentalGroup.ifPresent(sampleInfo::setExperimentalGroup);
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
        sampleInfo.biologicalReplicate = biologicalReplicate;
        sampleInfo.experimentalGroup = experimentalGroup;
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

  private static String formatConditionString(Condition condition) {
    return condition.getVariableLevels().parallelStream()
        .map(variableLevel -> "%s:%s %s"
            .formatted(variableLevel.variableName().value(),
                variableLevel.experimentalValue().value(),
                variableLevel.experimentalValue().unit().orElse(""))
            .trim())
        .sorted()
        .collect(Collectors.joining("; "));
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
    /* do nothing */
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
    for (SampleInfo sampleInfo : prefilledSampleInfos()) {
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
    private BiologicalReplicate biologicalReplicate;
    private ExperimentalGroup experimentalGroup;
    private Species species;
    private Specimen specimen;
    private Analyte analyte;
    private String customerComment;

    public static SampleInfo create(AnalysisMethod analysisMethod,
        String sampleLabel,
        BiologicalReplicate biologicalReplicate,
        ExperimentalGroup experimentalGroup,
        Species species,
        Specimen specimen,
        Analyte analyte,
        String customerComment) {
      SampleInfo sampleInfo = new SampleInfo();
      sampleInfo.setAnalysisToBePerformed(analysisMethod);
      sampleInfo.setSampleLabel(sampleLabel);
      sampleInfo.setExperimentalGroup(experimentalGroup);
      sampleInfo.setBiologicalReplicate(biologicalReplicate);
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

    public void setBiologicalReplicate(
        BiologicalReplicate biologicalReplicate) {
      this.biologicalReplicate = biologicalReplicate;
    }

    public BiologicalReplicate getBiologicalReplicate() {
      return biologicalReplicate;
    }

    public ExperimentalGroup getExperimentalGroup() {
      return experimentalGroup;
    }

    public void setExperimentalGroup(
        ExperimentalGroup experimentalGroup) {
      this.experimentalGroup = experimentalGroup;
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
          .add("biologicalReplicate=" + biologicalReplicate)
          .add("experimentalGroup=" + experimentalGroup)
          .add("species=" + species)
          .add("specimen=" + specimen)
          .add("analyte=" + analyte)
          .add("customerComment='" + customerComment + "'")
          .toString();
    }
  }

}
