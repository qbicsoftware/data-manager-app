package life.qbic.datamanager.files.export;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetVisibility;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

/**
 * Helps to create excel template sheets.
 * <p>
 * This class provides methods to set up validation, lock and hide sheets and a safe way to access
 * rows and cells in a sheet.
 *
 * @since 1.5.0
 */
public class XLSXTemplateHelper {

  private static final Random RANDOM = new SecureRandom();
  private static final byte[] DARK_GREY = {(byte) 119, (byte) 119, (byte) 119};
  private static final byte[] LIGHT_GREY = {(byte) 220, (byte) 220, (byte) 220};
  private static final byte[] LINK_BLUE = {(byte) 9, (byte) 105, (byte) 218};
  private static final String PROPERTY_INFORMATION_SHEET_NAME = "Property Information";
  private static final String DEFAULT_FONT = "Open Sans";

  protected XLSXTemplateHelper() {
    //hide constructor as static methods only are used
  }

  /**
   * Asks for a specific row with an index starting with index 0 for the first row. If no row
   * exists, creates a new and empty row.
   *
   * @param sheet    the sheet to ask for the row
   * @param rowIndex the index of the row starting with 0 for the first row.
   * @return the row in the sheet
   * @since 1.5.0
   */
  public static Row getOrCreateRow(Sheet sheet, int rowIndex) {
    Row row = sheet.getRow(rowIndex);
    if (isNull(row)) {
      row = sheet.createRow(rowIndex);
    }
    return row;
  }

  /**
   * Asks for a specific cell with a column index starting with index 0 for the first column. If no
   * cell exists, creates a new and blank cell.
   *
   * @param row       the row to ask for the cell
   * @param cellIndex the column index of the cell in the row. Starting with 0 for the first
   *                  column.
   * @return the cell in the row
   * @since 1.5.0
   */
  public static Cell getOrCreateCell(Row row, int cellIndex) {
    Cell cell = row.getCell(cellIndex);
    if (nonNull(cell)) {
      return cell;
    }
    return row.createCell(cellIndex);
  }

  /**
   * The value within the cell as String
   *
   * @param cell the cell to read the value from, must not be null.
   * @return the value of the cell. Never null.
   * @since 1.5.0
   */
  public static String getCellValueAsString(Cell cell) {
    if (isNull(cell)) {
      throw new IllegalArgumentException("cell is null");
    }
    return switch (cell.getCellType()) {
      case FORMULA, _NONE, BLANK, ERROR -> "";
      case STRING -> cell.getStringCellValue();
      case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
      case NUMERIC -> Double.toString(cell.getNumericCellValue());
    };
  }

  static Optional<CellStyle> findBoldCellStyle(Sheet sheet) {
    for (int i = 0; i < sheet.getWorkbook().getNumCellStyles(); i++) {
      CellStyle cellStyleAt = sheet.getWorkbook().getCellStyleAt(i);
      int fontIndex = cellStyleAt.getFontIndex();
      Font fontAt = sheet.getWorkbook().getFontAt(fontIndex);
      if (fontAt.getBold()) {
        return Optional.of(cellStyleAt);
      }
    }
    return Optional.empty();
  }

  public static void autoSizeAllColumns(Sheet sheet, int columnIndexStart, int columnIndexEnd,
      int maxRowIdx, IntFunction<Optional<String>> longestValueForColumn, CellStyle cellStyle) {
    Row tempRow = sheet.createRow(maxRowIdx + 1);
    for (int currentColumnIndex = columnIndexStart; currentColumnIndex <= columnIndexEnd;
        currentColumnIndex++) {
      String longestValue = longestValueForColumn.apply(currentColumnIndex).orElse("");
      Cell cell = tempRow.createCell(currentColumnIndex);
      cell.setCellValue(longestValue);
      cell.setCellStyle(cellStyle);
      sheet.autoSizeColumn(currentColumnIndex);
    }
    sheet.removeRow(tempRow);
  }


  public static CellStyle createBoldCellStyle(Workbook workbook) {
    CellStyle boldStyle = workbook.createCellStyle();
    Font fontBold = workbook.createFont();
    fontBold.setBold(true);
    fontBold.setFontName(DEFAULT_FONT);
    fontBold.setFontHeightInPoints((short) 12);

    boldStyle.setFont(fontBold);
    return boldStyle;
  }

  public static CellStyle createDefaultCellStyle(Workbook workbook) {
    CellStyle boldStyle = workbook.createCellStyle();
    Font fontBold = workbook.createFont();
    fontBold.setFontName(DEFAULT_FONT);
    fontBold.setFontHeightInPoints((short) 12);

    boldStyle.setFont(fontBold);
    return boldStyle;
  }

  public static CellStyle createLinkHeaderCellStyle(Workbook workbook) {
    CellStyle linkHeaderStyle = workbook.createCellStyle();
    XSSFFont linkFont = (XSSFFont) workbook.createFont();
    linkFont.setColor(new XSSFColor(LINK_BLUE, new DefaultIndexedColorMap()));
    linkFont.setBold(true);
    linkFont.setFontName(DEFAULT_FONT);
    linkFont.setFontHeightInPoints((short) 12);

    linkHeaderStyle.setFont(linkFont);
    return linkHeaderStyle;
  }

  public static CellStyle createReadOnlyCellStyle(Workbook workbook) {
    CellStyle readOnlyStyle = workbook.createCellStyle();
    readOnlyStyle.setFillForegroundColor(new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
    readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    XSSFFont font = (XSSFFont) workbook.createFont();
    font.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
    font.setFontName(DEFAULT_FONT);
    font.setFontHeightInPoints((short) 12);
    readOnlyStyle.setFont(font);
    return readOnlyStyle;
  }

  public static CellStyle createReadOnlyHeaderCellStyle(Workbook workbook) {
    CellStyle readOnlyHeaderStyle = workbook.createCellStyle();
    readOnlyHeaderStyle.setFillForegroundColor(
        new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
    readOnlyHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    XSSFFont fontHeader = (XSSFFont) workbook.createFont();
    fontHeader.setBold(true);
    fontHeader.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
    fontHeader.setFontName(DEFAULT_FONT);
    fontHeader.setFontHeightInPoints((short) 12);
    readOnlyHeaderStyle.setFont(fontHeader);
    return readOnlyHeaderStyle;
  }

  /**
   * Adds values to the sheet and returns the named area where they were added.
   *
   * @param sheet        the sheet where to add the values
   * @param propertyName the name of the property
   * @param options      the available options to choose a value from
   * @return a defined name for a range of cells in the workbook.
   * @see Name
   * @since 1.5.0
   */
  public static Name createOptionArea(Sheet sheet, String propertyName,
      List<String> options) {
    Row headerRow = getOrCreateRow(sheet, 0);
    var columnNumber = Math.max(1,
        headerRow.getLastCellNum() + 1); // the column to use for the property. Starts with 1
    var columnIndex = columnNumber - 1;

    // create header cell
    Cell headerRowCell = headerRow.createCell(columnIndex);
    headerRowCell.setCellValue(propertyName);

    //if a bold cell style exists, use it
    findBoldCellStyle(sheet).ifPresent(headerRowCell::setCellStyle);

    var startIndex = 1; // ignore the header at 0
    var rowIndex = startIndex;
    for (String option : options) {
      Row valueRow = getOrCreateRow(sheet, rowIndex);
      getOrCreateCell(valueRow, columnIndex)
          .setCellValue(option);
      rowIndex++;
    }
    var reference = "'%s'!$%s$%s:$%s$%s".formatted( //e.g. 'My Sheet'!$A$2:$E$23
        sheet.getSheetName(),
        CellReference.convertNumToColString(columnIndex),
        1 + startIndex, //shift by start index
        CellReference.convertNumToColString(columnIndex),
        options.size() + startIndex //shift by start index
    );
    var namedArea = sheet.getWorkbook().createName();

    namedArea.setNameName(toCamelCase(propertyName));
    namedArea.setRefersToFormula(reference);
    return namedArea;
  }

  /**
   * Converts a string to camel case. Leaves the first character as is. Considers non-word
   * characters as well as underscores to be word separators.
   * <p>
   * For example: "this is a sentence" and "this_is_a_string" become "thisIsASentence"
   *
   * @param input the input to camel case
   * @return a camel case representation of the input
   * @since 1.5.0
   */
  public static String toCamelCase(String input) {
    StringBuilder stringBuilder = new StringBuilder(input);
    Predicate<Character> isWordSeparator = character -> String.valueOf(character).matches("\\W|_");
    for (int i = 0; i < stringBuilder.length(); i++) {
      if (isWordSeparator.test(stringBuilder.charAt(i))) {
        stringBuilder.deleteCharAt(
            i); //remove the separator shifting the next character into position i
        if (stringBuilder.length() <= i) {
          //the last character was removed
          break;
        }
        stringBuilder.replace(i, i + 1,
            String.valueOf(stringBuilder.charAt(i)).toUpperCase());//capitalize next character
      }
    }
    return stringBuilder.toString();
  }

  /**
   * Adds data validation to an area in the spreadsheet. Requires the valid options to be set
   * beforehand as a name. This can be done by using {@link #createOptionArea(Sheet, String, List)}
   * <p>
   * Please note: There must not exist any data validation for the cell area provided.
   *
   * @param sheet         the sheet in which the validation should be added
   * @param startColIdx   the start column of the validated values >= 0
   * @param startRowIdx   the start row of the validated values >= 0
   * @param stopColIdx    the last column of the validated values >= startColIdx
   * @param stopRowIdx    the last row of the validated values >= startRowIdx
   * @param allowedValues the named area defining the allowed values
   * @since 1.5.0
   */
  public static void addDataValidation(Sheet sheet, int startColIdx, int startRowIdx,
      int stopColIdx, int stopRowIdx, Name allowedValues) {
    CellRangeAddressList validatedCells = new CellRangeAddressList(startRowIdx,
        stopRowIdx,
        startColIdx,
        stopColIdx);

    if (hasAnyDataValidation(sheet, startRowIdx, startColIdx, stopRowIdx, stopColIdx)) {
      throw new IllegalStateException(
          "Cannot add data validation as there is already a data validation present at "
              + validatedCells.getCellRangeAddress(0).formatAsString(sheet.getSheetName(), true));
    }

    DataValidationHelper dataValidationHelper = sheet.getDataValidationHelper();
    DataValidationConstraint formulaListConstraint = dataValidationHelper
        .createFormulaListConstraint(allowedValues.getNameName());

    var validation = dataValidationHelper.createValidation(formulaListConstraint, validatedCells);

    validation.setSuppressDropDownArrow(true); // shows dropdown if true
    validation.setShowErrorBox(true);
    validation.createErrorBox("Invalid choice", "Please select a value from the dropdown list.");
    sheet.addValidationData(validation);
  }

  /**
   * Adds a property information description to the workbook. Creates a sheet called
   * {@link #PROPERTY_INFORMATION_SHEET_NAME} if it does not exist yet.
   *
   * @param workbook         the workbook to take
   * @param columnName       the column name / property name to add information to
   * @param isMandatory      is filling the column mandatory?
   * @param descriptionTitle allowed value type and example
   * @param description      the description of the input
   * @param headerStyle      the style used for headers in the property information sheet
   */
  public static void addPropertyInformation(Workbook workbook,
      String columnName,
      boolean isMandatory,
      String descriptionTitle,
      String description,
      CellStyle defaultStyle,
      CellStyle headerStyle) {
    // add row with information
    Sheet propertyInformationSheet = Optional
        .ofNullable(workbook.getSheet(PROPERTY_INFORMATION_SHEET_NAME))
        .orElseGet(() -> workbook.createSheet(PROPERTY_INFORMATION_SHEET_NAME));
    int lastRowIdx = Math.max(propertyInformationSheet.getLastRowNum(), 0);
    if (lastRowIdx == 0) {
      //we do not have a header yet
      Row row = getOrCreateRow(propertyInformationSheet, 0);
      Cell propertyNameCell = getOrCreateCell(row, 0);
      propertyNameCell.setCellStyle(headerStyle);
      propertyNameCell.setCellValue("Property Name");

      Cell provisionCell = getOrCreateCell(row, 1);
      provisionCell.setCellStyle(headerStyle);
      provisionCell.setCellValue("Provision");

      Cell allowedValuesCell = getOrCreateCell(row, 2);
      allowedValuesCell.setCellStyle(headerStyle);
      allowedValuesCell.setCellValue("Allowed Values");

      Cell descriptionCell = getOrCreateCell(row, 3);
      descriptionCell.setCellStyle(headerStyle);
      descriptionCell.setCellValue("Description");

    }
    lastRowIdx++;
    Row row = getOrCreateRow(propertyInformationSheet, lastRowIdx);
    Cell propertyNameCell = getOrCreateCell(row, 0);
    propertyNameCell.setCellStyle(defaultStyle);
    propertyNameCell.setCellValue(columnName);

    Cell provisionCell = getOrCreateCell(row, 1);
    provisionCell.setCellStyle(defaultStyle);
    provisionCell.setCellValue(isMandatory ? "mandatory" : "optional");

    Cell allowedValuesCell = getOrCreateCell(row, 2);
    allowedValuesCell.setCellStyle(defaultStyle);
    allowedValuesCell.setCellValue(descriptionTitle);

    Cell descriptionCell = getOrCreateCell(row, 3);
    descriptionCell.setCellStyle(defaultStyle);
    descriptionCell.setCellValue(description);

    autoSizeAllColumns(propertyInformationSheet, 0, 3, lastRowIdx + 1, ignored -> Optional.empty(),
        defaultStyle);
  }


  /**
   * Adds an input prompt box to cells within the selected range. If there is already a validation
   * for exactly those cells, the prompt box of the existing validation is overwritten.
   *
   * @param sheet       the sheet in which the cells are
   * @param startColIdx the index of the first column
   * @param startRowIdx the index of the first row
   * @param stopColIdx  the index of the last column
   * @param stopRowIdx  the index of the last row
   * @param title       the title of the message in the prompt box
   * @param content     the content of the prompt box
   */
  public static void addInputHelper(Sheet sheet, int startColIdx, int startRowIdx,
      int stopColIdx, int stopRowIdx, String title, String content) {
    CellRangeAddressList validatedCells = new CellRangeAddressList(startRowIdx,
        stopRowIdx,
        startColIdx,
        stopColIdx);

    var validation = getValidationsExactlyCovering(sheet, validatedCells.getCellRangeAddresses()[0])
        .stream()
        .findFirst() // the first is applied first
        .orElse(createFakeValidation(sheet, validatedCells));

    validation.setShowPromptBox(true);
    validation.createPromptBox(title, content);
    sheet.addValidationData(validation);
  }

  private static DataValidation createFakeValidation(Sheet sheet,
      CellRangeAddressList validatedCells) {
    DataValidationHelper dataValidationHelper = sheet.getDataValidationHelper();
    DataValidationConstraint alwaysTrue = dataValidationHelper.createCustomConstraint("TRUE");
    return dataValidationHelper.createValidation(alwaysTrue,
        validatedCells);
  }

  private static List<DataValidation> getValidationsExactlyCovering(Sheet sheet,
      CellRangeAddress cellRangeAddress) {
    List<DataValidation> validations = new ArrayList<>();
    for (DataValidation dataValidation : sheet.getDataValidations()) {
      for (CellRangeAddress rangeAddress : dataValidation.getRegions().getCellRangeAddresses()) {
        if (rangeAddress.equals(cellRangeAddress)) {
          validations.add(dataValidation);
        }
      }
    }
    return validations;
  }

  private static boolean hasAnyDataValidation(Sheet sheet, int startRowIdx, int startColIdx,
      int stopRowIdx, int stopColIdx) {
    for (DataValidation dataValidation : sheet.getDataValidations()) {
      CellRangeAddressList regions = dataValidation.getRegions();
      for (int i = 0; i < regions.getCellRangeAddresses().length; i++) {
        CellRangeAddress cellRangeAddress = regions.getCellRangeAddress(i);
        if (cellRangeAddress.intersects(
            new CellRangeAddress(startRowIdx, stopRowIdx, startColIdx, stopColIdx))) {
          return true;
        }
      }
    }
    return false;

  }


  public static void hideSheet(Workbook workbook, Sheet sheet) {
    workbook.setSheetVisibility(workbook.getSheetIndex(sheet), SheetVisibility.VERY_HIDDEN);
  }

  public static void lockSheet(Sheet sheet) {
    String randomPassword = RANDOM.ints(16)
        .mapToObj(Integer::toString)
        .collect(Collectors.joining());
    sheet.protectSheet(randomPassword);
  }


}
