package life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;
import life.qbic.projectmanagement.application.sample.PropertyConversion;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.Column;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.WorkbookFactory;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.ConfoundingVariableColumn;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.column.InformationColumn;
import life.qbic.projectmanagement.infrastructure.template.provider.openxml.util.XLSXTemplateHelper;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

class SampleInformationFactory implements WorkbookFactory {

  private final List<Sample> samples;
  private final List<String> analysisMethods;
  private final List<String> conditions;
  private final List<String> analytes;
  private final List<String> species;
  private final List<String> specimen;
  private final List<ExperimentalGroup> experimentalGroups;
  private final List<ConfoundingVariableInformation> confoundingVariables;
  private final List<ConfoundingVariableLevel> confoundingVariableLevels;

  SampleInformationFactory(List<Sample> samples, List<String> analysisMethods,
      List<String> conditions, List<String> analytes, List<String> species,
      List<String> specimen, List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariableInformation> confoundingVariables,
      List<ConfoundingVariableLevel> confoundingVariableLevels) {
    this.samples = samples;
    this.analysisMethods = analysisMethods;
    this.conditions = conditions;
    this.analytes = analytes;
    this.species = species;
    this.specimen = specimen;
    this.experimentalGroups = experimentalGroups;
    this.confoundingVariables = confoundingVariables;
    this.confoundingVariableLevels = confoundingVariableLevels;
  }

  @Override
  public int numberOfRowsToGenerate() {
    return 2_000;
  }

  @Override
  public void enterValuesAsRows(Sheet sheet, CellStyles cellStyles) {
    int rowIndex = 1; //start in the second row with index 1.
    for (Sample sample : samples) {
      Row row = XLSXTemplateHelper.getOrCreateRow(sheet, rowIndex);
      var experimentalGroup = experimentalGroups.stream()
          .filter(group -> group.id() == sample.experimentalGroupId())
          .findFirst().orElseThrow();
      fillRowWithSampleMetadata(row, sample, experimentalGroup.condition(),
          cellStyles.defaultCellStyle(),
          cellStyles.readOnlyCellStyle());
      rowIndex++;
    }
  }

  @Override
  public String sheetName() {
    return "Sample Metadata";
  }

  @Override
  public Column[] getColumns() {

    ArrayList<Column> columns = new ArrayList<>(List.of(InformationColumn.values()));

    var colOffset = InformationColumn.maxColumnIndex() + 1; //offset + 0 is the next free column
    for (int i = 0; i < confoundingVariables.size(); i++) {
      var confoundingVariable = confoundingVariables.get(i);
      int columnIndex = colOffset + i;
      columns.add(
          new ConfoundingVariableColumn(confoundingVariable.id(), columnIndex,
              confoundingVariable.variableName()));
    }

    return columns.toArray(new Column[0]);
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    //nothing to do here
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (columnIndex == InformationColumn.ANALYSIS.index()) {
      return analysisMethods.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.CONDITION.index()) {
      return conditions.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.ANALYTE.index()) {
      return analytes.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.SPECIES.index()) {
      return species.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.SPECIMEN.index()) {
      return specimen.stream().reduce(keepLongerString);
    }
    return Optional.empty();
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    //nothing to do here
  }

  private void fillRowWithSampleMetadata(Row row, Sample sample,
      Condition condition, CellStyle defaultStyle, CellStyle readOnlyCellStyle) {

    for (Column column : getColumns()) {
      String value;
      if (column instanceof InformationColumn informationColumn) {
        value = switch (informationColumn) {
          case InformationColumn.SAMPLE_ID -> sample.sampleCode().code();
          case InformationColumn.ANALYSIS -> sample.analysisMethod().abbreviation();
          case InformationColumn.SAMPLE_NAME -> sample.label();
          case InformationColumn.BIOLOGICAL_REPLICATE -> sample.biologicalReplicate();
          case InformationColumn.CONDITION -> PropertyConversion.toString(condition);
          case InformationColumn.SPECIES -> PropertyConversion.toString(sample.sampleOrigin().getSpecies());
          case InformationColumn.ANALYTE -> PropertyConversion.toString(sample.sampleOrigin().getAnalyte());
          case InformationColumn.SPECIMEN -> PropertyConversion.toString(sample.sampleOrigin().getSpecimen());
          case InformationColumn.COMMENT -> sample.comment().orElse(null);
        };
      } else if (column instanceof ConfoundingVariableColumn confoundingVariableColumn) {
        VariableReference variableReference = confoundingVariableColumn.variableReference();
        value = confoundingVariableLevels.stream()
            .filter(level -> variableReference.equals(level.variable())
                && level.sample().id().equals(sample.sampleId().value()))
            .findFirst()
            .map(ConfoundingVariableLevel::level)
            .orElse(null);
      } else {
        throw new ApplicationException("Unexpected column type: " + column.getClass());
      }
      var cell = XLSXTemplateHelper.getOrCreateCell(row, column.index());
      cell.setCellValue(value);
      cell.setCellStyle(defaultStyle);
      if (column.isReadOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }
  }
}
