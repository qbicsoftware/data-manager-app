package life.qbic.datamanager.views.projects.project.samples.registration.batch;

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
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.AnalysisMethod;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;

/**
 * A spreadsheet used for sample batch information
 */
public class SampleBatchInformationSpreadsheet extends Spreadsheet<SampleInfo> {

  private static final int INITIAL_ROW_COUNT = 2;


  public SampleBatchInformationSpreadsheet(List<ExperimentalGroup> experimentalGroups,
      List<OntologyTerm> species, List<OntologyTerm> specimens,
      List<OntologyTerm> analytes, boolean showSampleCode) {
    List<AnalysisMethod> sortedAnalysisMethods = Arrays.stream(AnalysisMethod.values())
        .sorted(Comparator.comparing(AnalysisMethod::label)).toList();

    if (showSampleCode) {
      Column<SampleInfo, SampleCode> sampleCodeColumn = addColumn("Sample ID",
          SampleInfo::getSampleCode, SampleCode::code, (sampleInfo, sampleCodeString) -> {
            var sampleCode =
                sampleCodeString.isBlank() ? null : SampleCode.create(sampleCodeString);
            sampleInfo.setSampleCode(sampleCode);
          })
          .requireDistinctValues();
      lockColumn(sampleCodeColumn);
    }

    addColumn("Analysis to be performed", SampleInfo::getAnalysisToBePerformed,
        AnalysisMethod::label,
        (sampleInfo, analysisToBePerformed) -> sampleInfo.setAnalysisToBePerformed(
            AnalysisMethod.forLabel(analysisToBePerformed)))
        .selectFrom(sortedAnalysisMethods, identity(), getAnalysisMethodItemRenderer())
        .setRequired();

    addColumn("Sample Name", SampleInfo::getSampleName, SampleInfo::setSampleName)
        .requireDistinctValues()
        .setRequired();

    addColumn("Biological Replicate", SampleInfo::getBiologicalReplicate,
        SampleInfo::setBiologicalReplicate);

    addColumn("Condition", SampleInfo::getExperimentalGroup,
        experimentalGroup -> formatConditionString(experimentalGroup.condition()),
        (sampleInfo, conditionString) -> updateSampleInfoWithMatchingExperimentalGroup(
            experimentalGroups, sampleInfo, conditionString))
        .selectFrom(experimentalGroups, identity())
        .setRequired();

    addColumn("Species", SampleInfo::getSpecies, OntologyTerm::getLabel,
        (sampleInfo, label) -> sampleInfo.setSpecies(findOntologyForLabel(species, label)))
        .selectFrom(species, identity())
        .setRequired();

    addColumn("Specimen", SampleInfo::getSpecimen, OntologyTerm::getLabel,
        (sampleInfo, label) -> sampleInfo.setSpecimen(findOntologyForLabel(specimens, label)))
        .selectFrom(specimens, identity())
        .setRequired();

    addColumn("Analyte", SampleInfo::getAnalyte, OntologyTerm::getLabel,
        (sampleInfo, label) -> sampleInfo.setAnalyte(findOntologyForLabel(analytes, label)))
        .selectFrom(analytes, identity())
        .setRequired();

    addColumn("Customer comment", SampleInfo::getCustomerComment, SampleInfo::setCustomerComment);

    resetRows(); //ensures the current column information is taken
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
    private String sampleName;
    private String biologicalReplicate;
    private ExperimentalGroup experimentalGroup;
    private OntologyTerm species;
    private OntologyTerm specimen;
    private OntologyTerm analyte;
    private String customerComment;

    public static SampleInfo create(AnalysisMethod analysisMethod, String sampleName,
        String biologicalReplicate, ExperimentalGroup experimentalGroup, OntologyTerm species,
        OntologyTerm specimen,
        OntologyTerm analyte, String customerComment) {
      return create(null, null, analysisMethod, sampleName, biologicalReplicate,
          experimentalGroup, species, specimen, analyte, customerComment);
    }

    public static SampleInfo create(SampleId sampleId, SampleCode sampleCode,
        AnalysisMethod analysisMethod, String sampleName, String biologicalReplicate,
        ExperimentalGroup experimentalGroup,
        OntologyTerm species, OntologyTerm specimen, OntologyTerm analyte,
        String customerComment) {
      SampleInfo sampleInfo = new SampleInfo();
      sampleInfo.setSampleId(sampleId);
      sampleInfo.setSampleCode(sampleCode);
      sampleInfo.setAnalysisToBePerformed(analysisMethod);
      sampleInfo.setSampleName(sampleName);
      sampleInfo.setBiologicalReplicate(biologicalReplicate);
      sampleInfo.setExperimentalGroup(experimentalGroup);
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

    public String getSampleName() {
      return sampleName;
    }

    public void setSampleName(String sampleName) {
      this.sampleName = sampleName;
    }

    public String getBiologicalReplicate() {
      return biologicalReplicate;
    }

    public void setBiologicalReplicate(String biologicalReplicate) {
      this.biologicalReplicate = biologicalReplicate;
    }

    public OntologyTerm getSpecies() {
      return species;
    }

    public void setSpecies(OntologyTerm species) {
      this.species = species;
    }

    public OntologyTerm getSpecimen() {
      return specimen;
    }

    public void setSpecimen(OntologyTerm specimen) {
      this.specimen = specimen;
    }

    public ExperimentalGroup getExperimentalGroup() {
      return experimentalGroup;
    }

    public void setExperimentalGroup(ExperimentalGroup experimentalGroup) {
      this.experimentalGroup = experimentalGroup;
    }

    public OntologyTerm getAnalyte() {
      return analyte;
    }

    public void setAnalyte(OntologyTerm analyte) {
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
      return new StringJoiner(", ", SampleInfo.class.getSimpleName() + "[", "]").add(
              "analysisToBePerformed=" + analysisToBePerformed).add("sampleName='" + sampleName + "'")
          .add("biologicalReplicate='" + biologicalReplicate + "'")
          .add("experimentalGroup=" + experimentalGroup).add("species=" + species)
          .add("specimen=" + specimen).add("analyte=" + analyte)
          .add("customerComment='" + customerComment + "'").toString();
    }

    public static SampleInfo copy(SampleInfo original) {
      SampleInfo sampleInfo = new SampleInfo();
      sampleInfo.analysisToBePerformed = original.analysisToBePerformed;
      sampleInfo.analyte = original.analyte;
      sampleInfo.customerComment = original.customerComment;
      sampleInfo.experimentalGroup = original.experimentalGroup;
      sampleInfo.sampleCode = original.sampleCode;
      sampleInfo.sampleId = original.sampleId;
      sampleInfo.sampleName = original.sampleName;
      sampleInfo.biologicalReplicate = original.biologicalReplicate;
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
      if (!Objects.equals(sampleName, that.sampleName)) {
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
      result = 31 * result + (sampleName != null ? sampleName.hashCode() : 0);
      result = 31 * result + (biologicalReplicate != null ? biologicalReplicate.hashCode() : 0);
      result = 31 * result + (experimentalGroup != null ? experimentalGroup.hashCode() : 0);
      result = 31 * result + (species != null ? species.hashCode() : 0);
      result = 31 * result + (specimen != null ? specimen.hashCode() : 0);
      result = 31 * result + (analyte != null ? analyte.hashCode() : 0);
      result = 31 * result + (customerComment != null ? customerComment.hashCode() : 0);
      return result;
    }
  }


  private static String formatConditionString(Condition condition) {
    return condition.getVariableLevels().parallelStream().map(
            variableLevel -> "%s:%s %s".formatted(variableLevel.variableName().value(),
                variableLevel.experimentalValue().value(),
                variableLevel.experimentalValue().unit().orElse("")).trim()).sorted()
        .collect(Collectors.joining("; "));
  }

  private static OntologyTerm findOntologyForLabel(List<OntologyTerm> selection,
      String label) {
    return selection.stream().filter(it -> it.getLabel().equals(label)).findFirst().orElse(null);
  }

  private static void updateSampleInfoWithMatchingExperimentalGroup(
      List<ExperimentalGroup> experimentalGroups, SampleInfo sampleInfo, String conditionString) {
    // find condition producing the same condition string
    Condition matchingCondition = experimentalGroups.stream().map(ExperimentalGroup::condition)
        .filter(it -> formatConditionString(it).equals(conditionString)).findAny().orElse(null);
    // find experimentalGroup with the condition
    Optional<ExperimentalGroup> matchingExperimentalGroup = experimentalGroups.stream()
        .filter(experimentalGroup -> experimentalGroup.condition().equals(matchingCondition))
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
