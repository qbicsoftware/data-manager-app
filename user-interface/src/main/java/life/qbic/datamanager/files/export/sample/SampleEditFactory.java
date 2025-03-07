package life.qbic.datamanager.files.export.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.files.export.WorkbookFactory;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateRow;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.sample.ConfoundingVariableColumn;
import life.qbic.datamanager.files.structure.sample.EditColumn;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariable;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableLevel;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.VariableReference;
import life.qbic.projectmanagement.application.sample.PropertyConversion;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class SampleEditFactory implements WorkbookFactory {

  private final List<Sample> samples;
  private final List<String> analysisMethods;
  private final List<String> conditions;
  private final List<String> analytes;
  private final List<String> species;
  private final List<String> specimen;
  private final List<ExperimentalGroup> experimentalGroups;
  private final List<ConfoundingVariable> confoundingVariables;
  private final List<ConfoundingVariableLevel> confoundingVariableLevels;

  public SampleEditFactory(List<Sample> samples, List<String> analysisMethods,
      List<String> conditions, List<String> analytes, List<String> species,
      List<String> specimen, List<ExperimentalGroup> experimentalGroups,
      List<ConfoundingVariable> confoundingVariables,
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
      Row row = getOrCreateRow(sheet, rowIndex);
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
    ArrayList<Column> columns = new ArrayList<>(List.of(EditColumn.values()));

    var colOffset = EditColumn.maxColumnIndex() + 1; //offset + 0 is the next free column
    for (int i = 0; i < confoundingVariables.size(); i++) {
      var confoundingVariable = confoundingVariables.get(i);
      int columnIndex = colOffset + i;
      columns.add(
          new ConfoundingVariableColumn(confoundingVariable.variableReference(), columnIndex,
              confoundingVariable.name()));
    }

    return columns.toArray(new Column[0]);
  }


  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    int maxIndex = numberOfRowsToGenerate() - 1;
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        maxIndex,
        EditColumn.ANALYSIS.index(),
        "Analysis Method",
        analysisMethods);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        maxIndex,
        EditColumn.CONDITION.index(),
        "Condition",
        conditions);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        maxIndex,
        EditColumn.ANALYTE.index(),
        "Analytes",
        analytes);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        maxIndex,
        EditColumn.SPECIES.index(),
        "Species",
        species);
    WorkbookFactory.addValidation(hiddenSheet,
        sheet,
        1,
        maxIndex,
        EditColumn.SPECIMEN.index(),
        "Specimen",
        specimen);
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (columnIndex == EditColumn.ANALYSIS.index()) {
      return analysisMethods.stream().reduce(keepLongerString);
    } else if (columnIndex == EditColumn.CONDITION.index()) {
      return conditions.stream().reduce(keepLongerString);
    } else if (columnIndex == EditColumn.ANALYTE.index()) {
      return analytes.stream().reduce(keepLongerString);
    } else if (columnIndex == EditColumn.SPECIES.index()) {
      return species.stream().reduce(keepLongerString);
    } else if (columnIndex == EditColumn.SPECIMEN.index()) {
      return specimen.stream().reduce(keepLongerString);
    }
    return Optional.empty();
  }
  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    // nothing to do
  }

  private void fillRowWithSampleMetadata(Row row, Sample sample,
      Condition condition, CellStyle defaultStyle, CellStyle readOnlyCellStyle) {

    for (Column column : getColumns()) {
      String value;
      if (column instanceof EditColumn editColumn) {
        value = switch (editColumn) {
          case SAMPLE_ID -> sample.sampleCode().code();
          case ANALYSIS -> sample.analysisMethod().abbreviation();
          case SAMPLE_NAME -> sample.label();
          case BIOLOGICAL_REPLICATE -> sample.biologicalReplicate();
          case CONDITION -> PropertyConversion.toString(condition);
          case SPECIES -> PropertyConversion.toString(sample.sampleOrigin().getSpecies());
          case ANALYTE -> PropertyConversion.toString(sample.sampleOrigin().getAnalyte());
          case SPECIMEN -> PropertyConversion.toString(sample.sampleOrigin().getSpecimen());
          case COMMENT -> sample.comment().orElse(null);
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
      var cell = getOrCreateCell(row, column.index());
      cell.setCellValue(value);
      cell.setCellStyle(defaultStyle);
      if (column.isReadOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }

  }
}
