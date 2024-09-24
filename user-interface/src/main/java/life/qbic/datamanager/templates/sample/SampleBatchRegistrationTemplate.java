package life.qbic.datamanager.templates.sample;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.setColumnAutoWidth;

import java.util.List;
import life.qbic.datamanager.parser.sample.RegisterColumns;
import life.qbic.datamanager.templates.XLSXTemplateHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class SampleBatchRegistrationTemplate {

  public static final int MAX_ROW_INDEX_TO = 2000;

  /**
   * Creates a template {@link XSSFWorkbook} for sample batch registration.
   * <p>
   * The client currently can expect that the workbook contains two {@link XSSFSheet}, accessible
   * via {@link XSSFWorkbook#getSheetAt(int)}.
   * <p>
   * At position 0, the sheet contains the actual template with column headers for the properties
   * expected for registration. If provided, some properties will also contain field validation
   * based on enumeration of selection choices.
   * <p>
   * This currently is true for the following properties:
   *
   * <ul>
   *   <li>Species</li>
   *   <li>Specimen</li>
   *   <li>Analyte</li>
   *   <li>Condition</li>
   *   <li>Analysis to perform</li>
   * </ul>
   * <p>
   * At position 1, the sheet is hidden and protected, containing fixed values for the field validation
   * in sheet 0. We don't want users to manually change them. They are given via the experimental
   * design. This is a compromise of using technical identifiers vs ease of use for property values.
   *
   * @param conditions all conditions available in the experiment to select from
   * @param species    all the species available in the experiment to select from
   * @param specimen   all the specimen available in the experiment to select from
   * @param analytes   all the analytes available in the experiment to select from
   * @return a pre-configured template workbook
   * @since 1.5.0
   */
  public static XSSFWorkbook createRegistrationTemplate(List<String> conditions,
      List<String> species, List<String> specimen, List<String> analytes,
      List<String> analysisToPerform) {
    XSSFWorkbook workbook = new XSSFWorkbook();
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

    var hiddenSheet = workbook.createSheet("hidden");
    Name analysisToBePerformedOptions = createOptionArea(hiddenSheet, "Analysis to be performed",
        analysisToPerform);
    Name conditionOptions = createOptionArea(hiddenSheet, "Condition", conditions);
    Name analyteOptions = createOptionArea(hiddenSheet, "Analyte", analytes);
    Name speciesOptions = createOptionArea(hiddenSheet, "Species", species);
    Name specimenOptions = createOptionArea(hiddenSheet, "Specimen", specimen);

    addDataValidation(sheet,
        RegisterColumns.ANALYSIS.columnIndex(),
        startIndex,
        RegisterColumns.ANALYSIS.columnIndex(),
        MAX_ROW_INDEX_TO,
        analysisToBePerformedOptions);
    addDataValidation(sheet,
        RegisterColumns.CONDITION.columnIndex(),
        startIndex,
        RegisterColumns.CONDITION.columnIndex(),
        MAX_ROW_INDEX_TO,
        conditionOptions);
    addDataValidation(sheet,
        RegisterColumns.ANALYTE.columnIndex(),
        startIndex,
        RegisterColumns.ANALYTE.columnIndex(),
        MAX_ROW_INDEX_TO,
        analyteOptions);
    addDataValidation(sheet,
        RegisterColumns.SPECIES.columnIndex(),
        startIndex,
        RegisterColumns.SPECIES.columnIndex(),
        MAX_ROW_INDEX_TO,
        speciesOptions);
    addDataValidation(sheet,
        RegisterColumns.SPECIMEN.columnIndex(),
        startIndex,
        RegisterColumns.SPECIMEN.columnIndex(),
        MAX_ROW_INDEX_TO,
        specimenOptions);

    setColumnAutoWidth(sheet, 0, RegisterColumns.maxColumnIndex());
    setColumnAutoWidth(hiddenSheet, 0, 4);
    workbook.setActiveSheet(0);
    lockSheet(hiddenSheet);
    hideSheet(workbook, hiddenSheet);

    return workbook;
  }
}
