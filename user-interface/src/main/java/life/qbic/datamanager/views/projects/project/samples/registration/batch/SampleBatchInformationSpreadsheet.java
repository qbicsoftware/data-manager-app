package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.isNull;
import static java.util.function.Function.identity;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;

/**
 * A spreadsheet used for sample batch information
 */
public class SampleBatchInformationSpreadsheet extends Spreadsheet<SampleInfo> {


  private final List<ExperimentalGroup> experimentalGroups;
  private final List<Species> species;
  private final List<Specimen> specimens;
  private final List<Analyte> analytes;

  private static final int INITIAL_ROW_COUNT = 2;


  public SampleBatchInformationSpreadsheet(List<ExperimentalGroup> experimentalGroups,
      List<Species> species, List<Specimen> specimens, List<Analyte> analytes) {
    this.experimentalGroups = new ArrayList<>(experimentalGroups);
    this.species = new ArrayList<>(species);
    this.specimens = new ArrayList<>(specimens);
    this.analytes = new ArrayList<>(analytes);
    List<AnalysisMethod> sortedAnalysisMethods = Arrays.stream(AnalysisMethod.values())
        .sorted(Comparator.comparing(AnalysisMethod::label))
        .toList();

    addColumn("Analysis to be performed",
        SampleInfo::getAnalysisToBePerformed,
        AnalysisMethod::label,
        (sampleInfo, analysisToBePerformed) -> sampleInfo.setAnalysisToBePerformed(
            AnalysisMethod.forLabel(analysisToBePerformed)))
        .selectFrom(sortedAnalysisMethods,
            identity(),
            getAnalysisMethodItemRenderer())
        .setRequired();

    addColumn("Sample label", SampleInfo::getSampleLabel,
        SampleInfo::setSampleLabel)
        .requireDistinctValues()
        .setRequired();

    addColumn("Condition", SampleInfo::getExperimentalGroup,
        experimentalGroup -> formatConditionString(experimentalGroup.condition()),
        (sampleInfo, conditionString) -> updateSampleInfoWithMatchingExperimentalGroup(
            experimentalGroups, sampleInfo, conditionString))
        .selectFrom(experimentalGroups, identity())
        .setRequired();

    addColumn("Biological replicate ID",
        SampleInfo::getBiologicalReplicate,
        BiologicalReplicate::label,
        (sampleInfo1, value) -> updateSampleInfoWithMatchingBiologicalReplicate(sampleInfo1, value,
            experimentalGroups))
        .selectFrom(sampleInfo -> Optional.ofNullable(sampleInfo.getExperimentalGroup())
            .map(ExperimentalGroup::biologicalReplicates).orElse(List.of()), identity())
        .setRequired();

    addColumn("Species",
        SampleInfo::getSpecies,
        Species::label,
        SampleInfo::setSpecies)
        .selectFrom(this.species, identity())
        .setRequired();

    addColumn("Specimen",
        SampleInfo::getSpecimen,
        Specimen::label,
        SampleInfo::setSpecimen)
        .selectFrom(this.specimens, identity())
        .setRequired();

    addColumn("Analyte",
        sampleInfo -> Optional.ofNullable(sampleInfo.getAnalyte())
            .map(Analyte::label)
            .orElse(null),
        SampleInfo::setAnalyte)
        .selectFrom(this.analytes, Analyte::label)
        .setRequired();

    addColumn("Customer comment", SampleInfo::getCustomerComment,
        SampleInfo::setCustomerComment);

    setValidationMode(ValidationMode.EAGER);

    for (int i = 0; i < INITIAL_ROW_COUNT; i++) {
      addEmptyRow();
    }
  }

  /**
   * adds an empty row to the spreadsheet
   */
  public void addEmptyRow() {
    ValidationMode validationMode = this.validationMode;
    setValidationMode(ValidationMode.LAZY);
    addRow(new SampleInfo());
    setValidationMode(validationMode);
  }

  public static class SampleInfo {

    private String sampleCode;
    private AnalysisMethod analysisToBePerformed;
    private String sampleLabel;
    private BiologicalReplicate biologicalReplicate;
    private ExperimentalGroup experimentalGroup;
    private Species species;
    private Specimen specimen;
    private Analyte analyte;
    private String customerComment;

    public static SampleInfo create(
        AnalysisMethod analysisMethod,
        String sampleLabel,
        BiologicalReplicate biologicalReplicate,
        ExperimentalGroup experimentalGroup,
        Species species,
        Specimen specimen,
        Analyte analyte,
        String customerComment) {
      return create(null, analysisMethod, sampleLabel, biologicalReplicate, experimentalGroup,
          species, specimen, analyte, customerComment);
    }

    public static SampleInfo create(
        String sampleCode,
        AnalysisMethod analysisMethod,
        String sampleLabel,
        BiologicalReplicate biologicalReplicate,
        ExperimentalGroup experimentalGroup,
        Species species,
        Specimen specimen,
        Analyte analyte,
        String customerComment) {
      SampleInfo sampleInfo = new SampleInfo();
      sampleInfo.setSampleCode(sampleCode);
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

    public Optional<String> getSampleCode() {
      return Optional.ofNullable(sampleCode);
    }

    public void setSampleCode(String sampleCode) {
      this.sampleCode = sampleCode;
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
      return new StringJoiner(", ", SampleInfo.class.getSimpleName() + "[",
          "]")
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


  private static void updateSampleInfoWithMatchingBiologicalReplicate(
      SampleInfo sampleInfo,
      String value, List<ExperimentalGroup> experimentalGroups) {

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

}
