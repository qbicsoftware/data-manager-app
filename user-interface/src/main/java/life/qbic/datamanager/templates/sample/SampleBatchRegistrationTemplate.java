package life.qbic.datamanager.templates.sample;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createBoldCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createDefaultCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createReadOnlyHeaderCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.setColumnAutoWidth;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.setColumnWidth;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.parser.ExampleProvider.Helper;
import life.qbic.datamanager.parser.sample.RegisterColumn;
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
      List<String> analysisToPerform, List<String> confoundingVariables) {
    XSSFWorkbook workbook = new XSSFWorkbook();
    var readOnlyHeaderStyle = createReadOnlyHeaderCellStyle(workbook);
    var boldCellStyle = createBoldCellStyle(workbook);
    var defaultStyle = createDefaultCellStyle(workbook);

    var sheet = workbook.createSheet("Sample Metadata");

    Row header = getOrCreateRow(sheet, 0);
    for (RegisterColumn column : RegisterColumn.values()) {
      var cell = getOrCreateCell(header, column.columnIndex());

      cell.setCellStyle(boldCellStyle);
      if (column.isMandatory()) {
        cell.setCellValue(column.headerName() + "*");
      } else {
        cell.setCellValue(column.headerName());
      }
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

    var columnOffset = RegisterColumn.maxColumnIndex();
    for (int confoundingVariableIndex = 0; confoundingVariableIndex < confoundingVariables.size();
        confoundingVariableIndex++) {
      String variableName = confoundingVariables.get(confoundingVariableIndex);
      int columnIndex = confoundingVariableIndex + columnOffset + 1;
      var cell = XLSXTemplateHelper.getOrCreateCell(header, columnIndex);
      cell.setCellValue(variableName);
      cell.setCellStyle(boldCellStyle);
    }

    // add property information order of columns matters!!
    for (RegisterColumn column : Arrays.stream(
            RegisterColumn.values())
        .sorted(Comparator.comparing(RegisterColumn::columnIndex)).toList()) {
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
    var helperStopIndex = 1; //stop in the second row with index 1

    var hiddenSheet = workbook.createSheet("hidden");
    Name analysisToBePerformedOptions = createOptionArea(hiddenSheet, "Analysis to be performed",
        analysisToPerform);
    Name conditionOptions = createOptionArea(hiddenSheet, "Condition", conditions);
    Name analyteOptions = createOptionArea(hiddenSheet, "Analyte", analytes);
    Name speciesOptions = createOptionArea(hiddenSheet, "Species", species);
    Name specimenOptions = createOptionArea(hiddenSheet, "Specimen", specimen);

    addDataValidation(sheet,
        RegisterColumn.ANALYSIS.columnIndex(),
        startIndex,
        RegisterColumn.ANALYSIS.columnIndex(),
        MAX_ROW_INDEX_TO,
        analysisToBePerformedOptions);
    addDataValidation(sheet,
        RegisterColumn.CONDITION.columnIndex(),
        startIndex,
        RegisterColumn.CONDITION.columnIndex(),
        MAX_ROW_INDEX_TO,
        conditionOptions);
    addDataValidation(sheet,
        RegisterColumn.ANALYTE.columnIndex(),
        startIndex,
        RegisterColumn.ANALYTE.columnIndex(),
        MAX_ROW_INDEX_TO,
        analyteOptions);
    addDataValidation(sheet,
        RegisterColumn.SPECIES.columnIndex(),
        startIndex,
        RegisterColumn.SPECIES.columnIndex(),
        MAX_ROW_INDEX_TO,
        speciesOptions);
    addDataValidation(sheet,
        RegisterColumn.SPECIMEN.columnIndex(),
        startIndex,
        RegisterColumn.SPECIMEN.columnIndex(),
        MAX_ROW_INDEX_TO,
        specimenOptions);

    for (var column : RegisterColumn.values()) {
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

    setColumnAutoWidth(sheet, 0, RegisterColumn.maxColumnIndex());
    // Auto width ignores cell validation values (e.g. a list of valid entries). So we need
    // to set them explicit
    setColumnWidth(sheet, RegisterColumn.CONDITION.columnIndex(), maxLength(conditions));
    setColumnWidth(sheet, RegisterColumn.SPECIES.columnIndex(), maxLength(species));
    setColumnWidth(sheet, RegisterColumn.SPECIMEN.columnIndex(), maxLength(specimen));
    setColumnWidth(sheet, RegisterColumn.ANALYTE.columnIndex(), maxLength(analytes));
    setColumnWidth(sheet, RegisterColumn.ANALYSIS.columnIndex(), maxLength(analysisToPerform));
    setColumnAutoWidth(hiddenSheet, 0, 4);
    workbook.setActiveSheet(0);
    lockSheet(hiddenSheet);
    hideSheet(workbook, hiddenSheet);

    return workbook;
  }

  static int maxLength(Collection<String> collection) {
    return collection.stream().mapToInt(String::length).max().orElse(0);
  }
}
