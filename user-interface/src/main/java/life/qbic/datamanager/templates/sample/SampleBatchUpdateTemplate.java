package life.qbic.datamanager.templates.sample;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createDefaultCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createReadOnlyCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.setColumnAutoWidth;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.parser.ExampleProvider.Helper;
import life.qbic.datamanager.parser.sample.EditColumn;
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
    var defaultStyle = createDefaultCellStyle(workbook);

    var sheet = workbook.createSheet("Sample Metadata");

    Row header = getOrCreateRow(sheet, 0);
    for (EditColumn column : EditColumn.values()) {
      var cell = XLSXTemplateHelper.getOrCreateCell(header, column.columnIndex());
      if (column.isMandatory()) {
        cell.setCellValue(column.headerName() + "*");
      } else {
        cell.setCellValue(column.headerName());
      }
      cell.setCellStyle(boldCellStyle);
      if (column.isReadOnly()) {
        cell.setCellStyle(readOnlyHeaderStyle);
      }
      //add helper to header
      column.getFillHelp().ifPresent(
          helper -> XLSXTemplateHelper.addInputHelper(sheet,
              column.columnIndex(),
              0,
              column.columnIndex(),
              0,
              helper.exampleValue(),
              helper.description()));
    }

    // add property information order of columns matters!!
    for (EditColumn column : Arrays.stream(
            EditColumn.values())
        .sorted(Comparator.comparing(EditColumn::columnIndex)).toList()) {
      // add property information
      var exampleValue = column.getFillHelp().map(Helper::exampleValue).orElse("");
      var description = column.getFillHelp().map(Helper::description).orElse("");
      XLSXTemplateHelper.addPropertyInformation(workbook,
          column.headerName(),
          column.isMandatory(),
          exampleValue,
          description,
          defaultStyle,
          boldCellStyle);
    }

    var startIndex = 1; //start in the second row with index 1.
    var helperStopIndex = 1; //stop in the second row with index 1.
    int rowIndex = startIndex;
    for (Sample sample : samples) {
      Row row = getOrCreateRow(sheet, rowIndex);
      var experimentalGroup = experimentalGroups.stream()
          .filter(group -> group.id() == sample.experimentalGroupId()).findFirst().orElseThrow();
      fillRowWithSampleMetadata(row, sample, experimentalGroup.condition(), defaultStyle,
          readOnlyCellStyle);
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
        EditColumn.ANALYSIS.columnIndex(),
        startIndex,
        EditColumn.ANALYSIS.columnIndex(),
        MAX_ROW_INDEX_TO,
        analysisToBePerformedOptions);
    addDataValidation(sheet,
        EditColumn.CONDITION.columnIndex(),
        startIndex,
        EditColumn.CONDITION.columnIndex(),
        MAX_ROW_INDEX_TO,
        conditionOptions);
    addDataValidation(sheet,
        EditColumn.ANALYTE.columnIndex(),
        startIndex,
        EditColumn.ANALYTE.columnIndex(),
        MAX_ROW_INDEX_TO,
        analyteOptions);
    addDataValidation(sheet,
        EditColumn.SPECIES.columnIndex(),
        startIndex,
        EditColumn.SPECIES.columnIndex(),
        MAX_ROW_INDEX_TO,
        speciesOptions);
    addDataValidation(sheet,
        EditColumn.SPECIMEN.columnIndex(),
        startIndex,
        EditColumn.SPECIMEN.columnIndex(),
        MAX_ROW_INDEX_TO,
        specimenOptions);

    for (var column : EditColumn.values()) {
      column.getFillHelp().ifPresent(
          helper -> XLSXTemplateHelper.addInputHelper(sheet,
              column.columnIndex(),
              startIndex,
              column.columnIndex(),
              helperStopIndex,
              helper.exampleValue(),
              helper.description())
      );
    }

    setColumnAutoWidth(sheet, 0, EditColumn.maxColumnIndex());
    workbook.setActiveSheet(0);
    lockSheet(hiddenSheet);
    hideSheet(workbook, hiddenSheet);

    return workbook;
  }

  private static void fillRowWithSampleMetadata(Row row, Sample sample,
      Condition condition, CellStyle defaultStyle, CellStyle readOnlyCellStyle) {
    for (EditColumn column : EditColumn.values()) {
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
      cell.setCellStyle(defaultStyle);
      if (column.isReadOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }
  }
}
