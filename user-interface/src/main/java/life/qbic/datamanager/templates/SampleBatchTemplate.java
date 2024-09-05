package life.qbic.datamanager.templates;

import java.util.List;
import life.qbic.datamanager.parser.PropertyToString;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
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

  private static int COLUMN_INDEX_ANALYSIS = 0;
  private static int COLUMN_INDEX_SAMPLE_NAME = 1;
  private static int COLUMN_INDEX_BIOLOGICAL_REPLICATE = 2;
  private static int COLUMN_INDEX_CONDITION = 3;
  private static int COLUMN_INDEX_SPECIES = 4;
  private static int COLUMN_INDEX_SPECIMEN = 5;
  private static int COLUMN_INDEX_ANALYTES = 6;
  private static int COLUMN_INDEX_COMMENT = 7;
  private static int COLUMN_INDEX_SAMPLE_ID = 8;

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

    // Analysis to perform
    setListDataTemplate(workbook.getSheetAt(1), 0, analysisToPerform);
    var rangedName = createNameForListReference(workbook, 1, analysisToPerform.size(), "ListSheet",
        "analysis", "A");
    setListDataValidation(workbook.getSheetAt(0), 1, 2000, 0, rangedName);
    setColumnHeader(workbook.getSheetAt(0), 0, "Analysis to be performed*");

    setColumnHeader(workbook.getSheetAt(0), 1, "Sample Name*");

    setColumnHeader(workbook.getSheetAt(0), 2, "Biological Replicate");

    setListDataTemplate(workbook.getSheetAt(1), 1, conditions);
    var rangedNameCondition = createNameForListReference(workbook, 1, conditions.size(),
        "ListSheet",
        "conditions", "B");
    setListDataValidation(workbook.getSheetAt(0), 1, 2000, 3, rangedNameCondition);
    setColumnHeader(workbook.getSheetAt(0), 3, "Condition*");

    setListDataTemplate(workbook.getSheetAt(1), 2, species);
    var rangedNameSpecies = createNameForListReference(workbook, 1, species.size(), "ListSheet",
        "species", "C");
    setListDataValidation(workbook.getSheetAt(0), 1, 2000, 4, rangedNameSpecies);
    setColumnHeader(workbook.getSheetAt(0), 4, "Species*");

    setListDataTemplate(workbook.getSheetAt(1), 3, specimen);
    var rangedNameSpecimen = createNameForListReference(workbook, 1, specimen.size(), "ListSheet",
        "specimen", "D");
    setListDataValidation(workbook.getSheetAt(0), 1, 2000, 5, rangedNameSpecimen);
    setColumnHeader(workbook.getSheetAt(0), 5, "Specimen*");

    setListDataTemplate(workbook.getSheetAt(1), 4, analytes);
    var rangedNameAnalyte = createNameForListReference(workbook, 1, analytes.size(), "ListSheet",
        "analyte", "E");
    setListDataValidation(workbook.getSheetAt(0), 1, 2000, 6, rangedNameAnalyte);
    setColumnHeader(workbook.getSheetAt(0), 6, "Analyte*");

    setColumnHeader(workbook.getSheetAt(0), 7, "Comment");

    setColumnAutoWidth(workbook.getSheetAt(0), 0, 20);
    setColumnAutoWidth(workbook.getSheetAt(1), 0, 20);

    return workbook;
  }

  /**
   * Sets a range of columns via {@link XSSFSheet#autoSizeColumn(int)} to set the width
   * automatically to the maximum cell value length observed in a column.
   *
   * @param sheet            the sheet for which the columns shall be adjusted
   * @param columnIndexStart the first column index to start the formatting
   * @param columnIndexEnd   the last column (inclusive) to have to formatting included
   * @since 1.5.0
   */
  private static void setColumnAutoWidth(XSSFSheet sheet, int columnIndexStart,
      int columnIndexEnd) {
    for (int currentColumn = columnIndexStart; currentColumn < columnIndexEnd;
        currentColumn++) {
      sheet.autoSizeColumn(currentColumn);
    }
  }

  private static void setColumnHeader(XSSFSheet sheet, int columnIndex, String header) {
    // Prevents override of a potential existing first row
    XSSFRow row = sheet.getRow(0) == null ? sheet.createRow(0) : sheet.getRow(0);
    row.createCell(columnIndex).setCellValue(header);
  }

  /**
   * Configures the template sheet with a column containing values that shall be part of a cell
   * validation via valid list values.
   *
   * @param sheet       the sheet that forms the template value sheet
   * @param columnIndex the index of the column to write the values to
   * @param values      the actual values for the list validation
   * @since 1.5.0
   */
  private static void setListDataTemplate(XSSFSheet sheet, int columnIndex,
      List<String> values) {
    int rowIndex = 0;
    for (String value : values) {
      XSSFRow row =
          sheet.getRow(rowIndex) == null ? sheet.createRow(rowIndex) : sheet.getRow(rowIndex);
      row.createCell(columnIndex).setCellValue(value);
      rowIndex++;
    }
  }

  /**
   * Sets a list range data validation for cells in a given sheet. The actual values have to be
   * provided in the referenced sheet from the list formula, the use of
   * {@link SampleBatchTemplate#setListDataTemplate(XSSFSheet, int, List)} is strongly recommended.
   *
   * @param sheet                 the sheet where the validation of cells shall happen
   * @param rowIndexFrom          the index of the row where to start
   * @param rowIndexTo            the index of the row where to stop validation (row with this index
   *                              will be still included)
   * @param columnIndexValidation the index of the column where to apply the validation (aka formula
   *                              list constraint)
   * @param listFormula           the actual list range formula, e.g. 'MySheet!$A$1:$A$30'
   * @since 1.5.0
   */
  private static void setListDataValidation(XSSFSheet sheet,
      int rowIndexFrom,
      int rowIndexTo,
      int columnIndexValidation,
      String listFormula) {
    DataValidationHelper dataValidationHelper = sheet.getDataValidationHelper();
    DataValidationConstraint dataValidationConstraint = dataValidationHelper.createFormulaListConstraint(
        listFormula);
    CellRangeAddressList cellRangeAddressList = new CellRangeAddressList(rowIndexFrom, rowIndexTo,
        columnIndexValidation, columnIndexValidation);
    DataValidation dataValidation = dataValidationHelper.createValidation(dataValidationConstraint,
        cellRangeAddressList);
    sheet.addValidationData(dataValidation);
  }

  private static String createNameForListReference(XSSFWorkbook workbook, int rangeStart,
      int rangeEnd,
      String sheetName, String nameName, String columnChar) {
    var name = workbook.createName();
    name.setNameName(nameName);
    String reference = "'%s'!$%s$%d:$%s$%d".formatted(sheetName, columnChar, rangeStart, columnChar,
        rangeEnd);
    name.setRefersToFormula(reference);
    return name.getNameName();
  }

  /**
   * Creates a {@link XSSFWorkbook} that contains a prefilled sheet of sample metadata based on the
   * provided list of {@link Sample}.
   * <p>
   * Cell validation will be configured exactly like in
   * {@link SampleBatchTemplate#createRegistrationTemplate(List, List, List, List, List)}.
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
    XSSFWorkbook workbook = createRegistrationTemplate(conditions, species, specimen, analytes,
        analysisToPerform);
    appendColumnHeader(workbook.getSheetAt(0), "Sample ID*");
    fillSheetWithSampleMetadata(workbook.getSheetAt(0), samples, experimentalGroups);
    setColumnAutoWidth(workbook.getSheetAt(0), 0, 20);
    return workbook;
  }

  private static void fillSheetWithSampleMetadata(XSSFSheet sheet, List<Sample> samples,
      List<ExperimentalGroup> experimentalGroups) {
    int rowIndex = 1; // 0 will contain the header!
    for (Sample sample : samples) {
      XSSFRow row = sheet.createRow(rowIndex);
      var experimentalGroup = experimentalGroups.stream()
          .filter(group -> group.id() == sample.experimentalGroupId()).findFirst().orElseThrow();
      fillRowWithSampleMetadata(row, sample, experimentalGroup);
      rowIndex++;
    }
  }

  private static void appendColumnHeader(XSSFSheet sheet, String header) {
    XSSFRow row = (XSSFRow) sheet.rowIterator().next();
    int indexNewColumn = row.getLastCellNum();
    row.createCell(indexNewColumn).setCellValue(header);
  }

  private static void fillRowWithSampleMetadata(XSSFRow row, Sample sample,
      ExperimentalGroup experimentalGroup) {
    row.createCell(COLUMN_INDEX_ANALYSIS).setCellValue(sample.analysisMethod().abbreviation());
    row.createCell(COLUMN_INDEX_SAMPLE_NAME).setCellValue(sample.label());
    row.createCell(COLUMN_INDEX_BIOLOGICAL_REPLICATE).setCellValue(sample.biologicalReplicate());
    row.createCell(COLUMN_INDEX_CONDITION)
        .setCellValue(PropertyToString.condition(experimentalGroup.condition()));
    row.createCell(COLUMN_INDEX_SPECIES)
        .setCellValue(PropertyToString.ontologyTerm(sample.sampleOrigin().getSpecies()));
    row.createCell(COLUMN_INDEX_SPECIMEN)
        .setCellValue(PropertyToString.ontologyTerm(sample.sampleOrigin().getSpecimen()));
    row.createCell(COLUMN_INDEX_ANALYTES)
        .setCellValue(PropertyToString.ontologyTerm(sample.sampleOrigin().getAnalyte()));
    row.createCell(COLUMN_INDEX_COMMENT).setCellValue(sample.comment().orElse(""));
    row.createCell(COLUMN_INDEX_SAMPLE_ID).setCellValue(sample.sampleCode().code());
  }
}
