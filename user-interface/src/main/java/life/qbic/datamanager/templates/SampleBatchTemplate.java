package life.qbic.datamanager.templates;

import java.util.List;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <b>Sample Batch Template</b>
 *
 * <p>Offers an API to create pre-configured workbooks for sample batch metadata use cases.</p>
 *
 * @since 1.5.0
 */
public class SampleBatchTemplate {

  /**
   * Creates a template {@link XSSFWorkbook} for sample batch registration.
   * <p>
   * The client currently can expect that the workbook contains two
   * {@link org.apache.poi.xssf.usermodel.XSSFSheet}, accessible via
   * {@link XSSFWorkbook#getSheetAt(int)}.
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
    workbook.createSheet();
    workbook.createSheet();
    workbook.setSheetName(0, "Sample Metadata");
    workbook.setSheetName(1, "ListSheet");
    workbook.setActiveSheet(0);

    setListDataTemplate(workbook.getSheetAt(1), 0, analysisToPerform);
    var rangedName = createNameForListReference(workbook, 1, analysisToPerform.size(), "ListSheet", "analysis", "A");
    setListDataValidation(workbook.getSheetAt(0), 1, 2000, 0, "Analysis to be performed*", rangedName);
    setSimpleColumn(workbook.getSheetAt(0), 1, "Sample Name*");
    setSimpleColumn(workbook.getSheetAt(0), 2, "Biological Replicate");
    return workbook;
  }

  private static void setListDataTemplate(XSSFSheet sheet, int columnIndex,
      List<String> values) {
    int row = 0;
    for (String value : values) {
      sheet.createRow(row).createCell(columnIndex).setCellValue(value);
      row++;
    }
  }

  private static void setSimpleColumn(XSSFSheet sheet, int columnIndex, String columnHeader) {
    XSSFRow row = sheet.getRow(0) == null ? sheet.createRow(0) : sheet.getRow(0);
    row.createCell(columnIndex).setCellValue(columnHeader);
  }

  private static void setListDataValidation(XSSFSheet metadataSheet, int rowIndexFrom,
      int rowIndexTo,
      int columnIndexValidation, String columnHeader,
      String listFormula) {
    XSSFRow row = metadataSheet.getRow(0) == null ? metadataSheet.createRow(0) : metadataSheet.getRow(0);
    row.createCell(columnIndexValidation).setCellValue(columnHeader);
    DataValidationHelper dataValidationHelper = metadataSheet.getDataValidationHelper();
    DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(
        listFormula);
    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(rowIndexFrom, rowIndexTo,
        columnIndexValidation, columnIndexValidation);
    DataValidation dataValidation = dataValidationHelper.createValidation(dataValidationConstraint,
        cellRangeAddressList);
    metadataSheet.addValidationData(dataValidation);
  }

  public static XSSFWorkbook createUpdateTemplate(List<Sample> samples) {
    return new XSSFWorkbook();
  }

  private static String createNameForListReference(XSSFWorkbook workbook, int rangeStart, int rangeEnd,
      String sheetName, String nameName, String columnChar) {
    var name = workbook.createName();
    name.setNameName(nameName);
    String reference = "'%s'!$%s$%d:$%s$%d".formatted(sheetName, columnChar, rangeStart, columnChar,
        rangeEnd);
    name.setRefersToFormula(reference);
    return name.getNameName();
  }
}
