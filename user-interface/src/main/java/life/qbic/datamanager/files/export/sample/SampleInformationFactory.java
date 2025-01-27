package life.qbic.datamanager.files.export.sample;

import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.files.export.XLSXTemplateHelper.getOrCreateRow;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import life.qbic.datamanager.files.export.WorkbookFactory;
import life.qbic.datamanager.files.structure.Column;
import life.qbic.datamanager.files.structure.sample.InformationColumn;
import life.qbic.projectmanagement.application.sample.PropertyConversion;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class SampleInformationFactory implements WorkbookFactory {

  private final List<Sample> samples;
  private final List<String> analysisMethods;
  private final List<String> conditions;
  private final List<String> analytes;
  private final List<String> species;
  private final List<String> specimen;
  private final List<ExperimentalGroup> experimentalGroups;

  public SampleInformationFactory(List<Sample> samples, List<String> analysisMethods,
      List<String> conditions, List<String> analytes, List<String> species,
      List<String> specimen, List<ExperimentalGroup> experimentalGroups) {
    this.samples = samples;
    this.analysisMethods = analysisMethods;
    this.conditions = conditions;
    this.analytes = analytes;
    this.species = species;
    this.specimen = specimen;
    this.experimentalGroups = experimentalGroups;
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
    return InformationColumn.values();
  }

  @Override
  public void customizeValidation(Sheet hiddenSheet, Sheet sheet) {
    //nothing to do here
  }

  @Override
  public Optional<String> longestValueForColumn(int columnIndex) {
    BinaryOperator<String> keepLongerString = (String s1, String s2) -> s1.length() > s2.length()
        ? s1 : s2;
    if (columnIndex == InformationColumn.ANALYSIS.getIndex()) {
      return analysisMethods.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.CONDITION.getIndex()) {
      return conditions.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.ANALYTE.getIndex()) {
      return analytes.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.SPECIES.getIndex()) {
      return species.stream().reduce(keepLongerString);
    } else if (columnIndex == InformationColumn.SPECIMEN.getIndex()) {
      return specimen.stream().reduce(keepLongerString);
    }
    return Optional.empty();
  }

  @Override
  public void customizeHeaderCells(Row header, CreationHelper creationHelper,
      CellStyles cellStyles) {
    //nothing to do here
  }

  private static void fillRowWithSampleMetadata(Row row, Sample sample,
      Condition condition, CellStyle defaultStyle, CellStyle readOnlyCellStyle) {
    for (InformationColumn column : InformationColumn.values()) {
      var value = switch (column) {
        case SAMPLE_ID -> sample.sampleCode().code();
        case ANALYSIS -> sample.analysisMethod().abbreviation();
        case SAMPLE_NAME -> sample.label();
        case BIOLOGICAL_REPLICATE -> sample.biologicalReplicate();
        case CONDITION -> PropertyConversion.toString(condition);
        case SPECIES -> PropertyConversion.toString(sample.sampleOrigin().getSpecies());
        case ANALYTE -> PropertyConversion.toString(sample.sampleOrigin().getAnalyte());
        case SPECIMEN -> PropertyConversion.toString(sample.sampleOrigin().getSpecimen());
        case COMMENT -> sample.comment().orElse("");
      };
      var cell = getOrCreateCell(row, column.getIndex());
      cell.setCellValue(value);
      cell.setCellStyle(defaultStyle);
      if (column.isReadOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }
  }
}
