package life.qbic.datamanager.exporting.xlsx.templates.sample;

import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.createBoldCellStyle;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.createDefaultCellStyle;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.createReadOnlyHeaderCellStyle;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.setColumnAutoWidth;
import static life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper.setColumnWidth;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import life.qbic.datamanager.exporting.xlsx.templates.XLSXTemplateHelper;
import life.qbic.datamanager.files.structure.sample.RegisterColumn;
import life.qbic.datamanager.importing.parser.ExampleProvider.Helper;
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
    var readOnlyHeaderStyle = createReadOnlyHeaderCellStyle(workbook);
    var boldCellStyle = createBoldCellStyle(workbook);
    var defaultStyle = createDefaultCellStyle(workbook);

    var sheet = workbook.createSheet("Sample Metadata");

    Row header = getOrCreateRow(sheet, 0);
    for (RegisterColumn column : RegisterColumn.values()) {
      var cell = getOrCreateCell(header, column.getIndex());

      cell.setCellStyle(boldCellStyle);
      if (column.isMandatory()) {
        cell.setCellValue(column.getName() + "*");
      } else {
        cell.setCellValue(column.getName());
      }
      if (column.isReadOnly()) {
        cell.setCellStyle(readOnlyHeaderStyle);
      }

      //add helper to header
      column.getFillHelp().ifPresent(
          helper -> XLSXTemplateHelper.addInputHelper(sheet,
              column.getIndex(),
              0,
              column.getIndex(),
              0,
              helper.exampleValue(),
              helper.description()));
    }

    // add property information order of columns matters!!
    for (RegisterColumn column : Arrays.stream(
            RegisterColumn.values())
        .sorted(Comparator.comparing(RegisterColumn::getIndex)).toList()) {
      // add property information
      var exampleValue = column.getFillHelp().map(Helper::exampleValue).orElse("");
      var description = column.getFillHelp().map(Helper::description).orElse("");
      XLSXTemplateHelper.addPropertyInformation(workbook,
          column.getName(),
          column.isMandatory(),
          exampleValue,
          description,
          defaultStyle,
          boldCellStyle);
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
        RegisterColumn.ANALYSIS.getIndex(),
        startIndex,
        RegisterColumn.ANALYSIS.getIndex(),
        MAX_ROW_INDEX_TO,
        analysisToBePerformedOptions);
    addDataValidation(sheet,
        RegisterColumn.CONDITION.getIndex(),
        startIndex,
        RegisterColumn.CONDITION.getIndex(),
        MAX_ROW_INDEX_TO,
        conditionOptions);
    addDataValidation(sheet,
        RegisterColumn.ANALYTE.getIndex(),
        startIndex,
        RegisterColumn.ANALYTE.getIndex(),
        MAX_ROW_INDEX_TO,
        analyteOptions);
    addDataValidation(sheet,
        RegisterColumn.SPECIES.getIndex(),
        startIndex,
        RegisterColumn.SPECIES.getIndex(),
        MAX_ROW_INDEX_TO,
        speciesOptions);
    addDataValidation(sheet,
        RegisterColumn.SPECIMEN.getIndex(),
        startIndex,
        RegisterColumn.SPECIMEN.getIndex(),
        MAX_ROW_INDEX_TO,
        specimenOptions);

    setColumnAutoWidth(sheet, 0, RegisterColumn.maxColumnIndex());
    // Auto width ignores cell validation values (e.g. a list of valid entries). So we need
    // to set them explicit
    setColumnWidth(sheet, RegisterColumn.CONDITION.getIndex(), maxLength(conditions));
    setColumnWidth(sheet, RegisterColumn.SPECIES.getIndex(), maxLength(species));
    setColumnWidth(sheet, RegisterColumn.SPECIMEN.getIndex(), maxLength(specimen));
    setColumnWidth(sheet, RegisterColumn.ANALYTE.getIndex(), maxLength(analytes));
    setColumnWidth(sheet, RegisterColumn.ANALYSIS.getIndex(), maxLength(analysisToPerform));
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
