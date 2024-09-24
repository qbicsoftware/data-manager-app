package life.qbic.datamanager.templates.sample;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createReadOnlyCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.setColumnAutoWidth;

import java.util.List;
import life.qbic.datamanager.parser.sample.EditColumns;
import life.qbic.datamanager.parser.sample.RegisterColumns;
import life.qbic.datamanager.templates.XLSXTemplateHelper;
import life.qbic.projectmanagement.application.sample.PropertyConversion;
import life.qbic.projectmanagement.domain.model.experiment.Condition;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <b>Sample Batch Template</b>
 *
 * <p>Offers an API to create pre-configured workbooks for sample batch metadata use cases.</p>
 *
 * @since 1.5.0
 */
public class SampleBatchUpdateTemplate {

  public static final int MAX_ROW_INDEX_TO = 2000;



  /**
   * Creates a {@link XSSFWorkbook} that contains a prefilled sheet of sample metadata based on the
   * provided list of {@link Sample}.
   *
   * @param samples            a list of samples with metadata that will be used to fill the
   *                           spreadsheet
   * @param conditions         a list of conditions that are available for selection
   * @param species            a list of species that are available for selection
   * @param specimen           a list of specimen that are available for selection
   * @param analytes           a list of analytes that are available for selection
   * @param analysisToPerform  a list of analysis types available for the sample measurement
   * @param experimentalGroups a list of experimental groups the samples belong to
   * @return a workbook with a prefilled sheet at tab index 0 that contains the sample metadata
   * @since 1.5.0
   */
  public static XSSFWorkbook createUpdateTemplate(List<Sample> samples, List<String> conditions,
      List<String> species, List<String> specimen, List<String> analytes,
      List<String> analysisToPerform, List<ExperimentalGroup> experimentalGroups) {

    XSSFWorkbook workbook = new XSSFWorkbook();
    var readOnlyCellStyle = createReadOnlyCellStyle(workbook);
    var readOnlyHeaderStyle = XLSXTemplateHelper.createReadOnlyHeaderCellStyle(workbook);
    var boldCellStyle = XLSXTemplateHelper.createBoldCellStyle(workbook);

    var sheet = workbook.createSheet("Sample Metadata");

    Row header = getOrCreateRow(sheet, 0);
    for (RegisterColumns column : RegisterColumns.values()) {
      var cell = XLSXTemplateHelper.getOrCreateCell(header, column.columnIndex());
      cell.setCellValue(column.headerName());
      cell.setCellStyle(boldCellStyle);
      if (column.readOnly()) {
        cell.setCellStyle(readOnlyHeaderStyle);
      }
    }
    var startIndex = 1; //start in the second row with index 1.
    int rowIndex = startIndex;
    for (Sample sample : samples) {
      Row row = getOrCreateRow(sheet, rowIndex);
      var experimentalGroup = experimentalGroups.stream()
          .filter(group -> group.id() == sample.experimentalGroupId()).findFirst().orElseThrow();
      fillRowWithSampleMetadata(row, sample, experimentalGroup.condition(), readOnlyCellStyle);
      rowIndex++;
    }

    var hiddenSheet = workbook.createSheet("hidden");
    Name analysisToBePerformedOptions = createOptionArea(hiddenSheet, "Analysis to be performed",
        analysisToPerform);
    Name conditionOptions = createOptionArea(hiddenSheet, "Condition", conditions);
    Name analyteOptions = createOptionArea(hiddenSheet, "Analyte", analytes);
    Name speciesOptions = createOptionArea(hiddenSheet, "Species", species);
    Name specimenOptions = createOptionArea(hiddenSheet, "Specimen", specimen);

    addDataValidation(sheet,
        EditColumns.ANALYSIS.columnIndex(),
        startIndex,
        EditColumns.ANALYSIS.columnIndex(),
        MAX_ROW_INDEX_TO,
        analysisToBePerformedOptions);
    addDataValidation(sheet,
        EditColumns.CONDITION.columnIndex(),
        startIndex,
        EditColumns.CONDITION.columnIndex(),
        MAX_ROW_INDEX_TO,
        conditionOptions);
    addDataValidation(sheet,
        EditColumns.ANALYTE.columnIndex(),
        startIndex,
        EditColumns.ANALYTE.columnIndex(),
        MAX_ROW_INDEX_TO,
        analyteOptions);
    addDataValidation(sheet,
        EditColumns.SPECIES.columnIndex(),
        startIndex,
        EditColumns.SPECIES.columnIndex(),
        MAX_ROW_INDEX_TO,
        speciesOptions);
    addDataValidation(sheet,
        EditColumns.SPECIMEN.columnIndex(),
        startIndex,
        EditColumns.SPECIMEN.columnIndex(),
        MAX_ROW_INDEX_TO,
        specimenOptions);

    setColumnAutoWidth(sheet, 0, EditColumns.maxColumnIndex());
    workbook.setActiveSheet(0);
    lockSheet(hiddenSheet);
    hideSheet(workbook, hiddenSheet);

    return workbook;
  }

  private static void fillRowWithSampleMetadata(Row row, Sample sample,
      Condition condition, CellStyle readOnlyCellStyle) {
    for (EditColumns column : EditColumns.values()) {
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
      var cell = getOrCreateCell(row, column.columnIndex());
      cell.setCellValue(value);
      if (column.readOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }
  }
}
