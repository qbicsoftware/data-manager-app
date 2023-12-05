package life.qbic.datamanager.views.projects.project.samples.registration.batch;

import static java.util.Objects.isNull;
import static java.util.function.Function.identity;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import life.qbic.datamanager.views.general.spreadsheet.Column;
import life.qbic.datamanager.views.general.spreadsheet.Spreadsheet;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleBatchInformationSpreadsheet.SampleInfo;
import life.qbic.projectmanagement.domain.model.experiment.BiologicalReplicate;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * A spreadsheet used for sample batch information
 */
public class SampleBatchInformationSpreadsheet extends Spreadsheet<SampleInfo> {

  private static final int INITIAL_ROW_COUNT = 2;


  public SampleBatchInformationSpreadsheet(List<ExperimentalGroup> experimentalGroups,
      List<Species> species, List<Specimen> specimens, List<Analyte> analytes,
      boolean showSampleCode) {
    List<AnalysisMethod> sortedAnalysisMethods = Arrays.stream(AnalysisMethod.values())
        .sorted(Comparator.comparing(AnalysisMethod::label))
        .toList();

    if (showSampleCode) {
      Column<SampleInfo, SampleCode> sampleCodeColumn = addColumn("Sample code",
          SampleInfo::getSampleCode,
          SampleCode::code,
          (sampleInfo, sampleCodeString) -> {
            var sampleCode =
                sampleCodeString.isBlank() ? null : SampleCode.create(sampleCodeString);
            sampleInfo.setSampleCode(sampleCode);
          })
          .requireDistinctValues();
      lockColumn(sampleCodeColumn);
    }

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
        .selectFrom(species, identity())
        .setRequired();

    addColumn("Specimen",
        SampleInfo::getSpecimen,
        Specimen::label,
        SampleInfo::setSpecimen)
        .selectFrom(specimens, identity())
        .setRequired();

    addColumn("Analyte",
        sampleInfo -> Optional.ofNullable(sampleInfo.getAnalyte())
            .map(Analyte::label)
            .orElse(null),
        SampleInfo::setAnalyte)
        .selectFrom(analytes, Analyte::label)
        .setRequired();

    addColumn("Customer comment", SampleInfo::getCustomerComment,
        SampleInfo::setCustomerComment);

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


    private SampleId sampleId;
    private SampleCode sampleCode;
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
      return create(null, null, analysisMethod, sampleLabel, biologicalReplicate, experimentalGroup,
          species, specimen, analyte, customerComment);
    }

    public static SampleInfo create(
        SampleId sampleId,
        SampleCode sampleCode,
        AnalysisMethod analysisMethod,
        String sampleLabel,
        BiologicalReplicate biologicalReplicate,
        ExperimentalGroup experimentalGroup,
        Species species,
        Specimen specimen,
        Analyte analyte,
        String customerComment) {
      SampleInfo sampleInfo = new SampleInfo();
      sampleInfo.setSampleId(sampleId);
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

    public void setSampleId(SampleId sampleId) {
      this.sampleId = sampleId;
    }

    public SampleId getSampleId() {
      return sampleId;
    }

    public SampleCode getSampleCode() {
      return sampleCode;
    }

    public void setSampleCode(SampleCode sampleCode) {
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

    public static SampleInfo copy(SampleInfo original) {
      SampleInfo sampleInfo = new SampleInfo();
      sampleInfo.analysisToBePerformed = original.analysisToBePerformed;
      sampleInfo.analyte = original.analyte;
      sampleInfo.biologicalReplicate = original.biologicalReplicate;
      sampleInfo.customerComment = original.customerComment;
      sampleInfo.experimentalGroup = original.experimentalGroup;
      sampleInfo.sampleCode = original.sampleCode;
      sampleInfo.sampleId = original.sampleId;
      sampleInfo.sampleLabel = original.sampleLabel;
      sampleInfo.species = original.species;
      sampleInfo.specimen = original.specimen;
      return sampleInfo;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      SampleInfo that = (SampleInfo) object;

      if (!Objects.equals(sampleId, that.sampleId)) {
        return false;
      }
      if (!Objects.equals(sampleCode, that.sampleCode)) {
        return false;
      }
      if (analysisToBePerformed != that.analysisToBePerformed) {
        return false;
      }
      if (!Objects.equals(sampleLabel, that.sampleLabel)) {
        return false;
      }
      if (!Objects.equals(biologicalReplicate, that.biologicalReplicate)) {
        return false;
      }
      if (!Objects.equals(experimentalGroup, that.experimentalGroup)) {
        return false;
      }
      if (!Objects.equals(species, that.species)) {
        return false;
      }
      if (!Objects.equals(specimen, that.specimen)) {
        return false;
      }
      if (!Objects.equals(analyte, that.analyte)) {
        return false;
      }
      return Objects.equals(customerComment, that.customerComment);
    }

    @Override
    public int hashCode() {
      int result = sampleId != null ? sampleId.hashCode() : 0;
      result = 31 * result + (sampleCode != null ? sampleCode.hashCode() : 0);
      result = 31 * result + (analysisToBePerformed != null ? analysisToBePerformed.hashCode() : 0);
      result = 31 * result + (sampleLabel != null ? sampleLabel.hashCode() : 0);
      result = 31 * result + (biologicalReplicate != null ? biologicalReplicate.hashCode() : 0);
      result = 31 * result + (experimentalGroup != null ? experimentalGroup.hashCode() : 0);
      result = 31 * result + (species != null ? species.hashCode() : 0);
      result = 31 * result + (specimen != null ? specimen.hashCode() : 0);
      result = 31 * result + (analyte != null ? analyte.hashCode() : 0);
      result = 31 * result + (customerComment != null ? customerComment.hashCode() : 0);
      return result;
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