package life.qbic.datamanager.templates;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
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

  private static final Random RANDOM = new Random();
  private static final byte[] DARK_GREY = {(byte) 119, (byte) 119, (byte) 119};
  private static final byte[] LIGHT_GREY = {(byte) 220, (byte) 220, (byte) 220};
  private static final int COLUMN_MAX_WIDTH = 255;

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

  /**
   * Sets a range of columns via {@link Sheet#autoSizeColumn(int)} to set the width automatically to
   * the maximum cell value length observed in a column.
   *
   * @param sheet            the sheet for which the columns shall be adjusted
   * @param columnIndexStart the first column index to start the formatting
   * @param columnIndexEnd   the last column (inclusive) to have to formatting included
   * @since 1.5.0
   */
  public static void setColumnAutoWidth(Sheet sheet, int columnIndexStart,
      int columnIndexEnd) {
    for (int currentColumn = columnIndexStart; currentColumn <= columnIndexEnd;
        currentColumn++) {
      sheet.autoSizeColumn(currentColumn);
    }
  }

  /**
   * Sets the width of a column explicitly. The width is expected to be the number in characters to
   * show.
   * <p>
   * Disclaimer: The current maximal value for the width is 255, since we inherit this restraint
   * from the underlying framework. See {@link Sheet#setColumnWidth(int, int)} for more.
   *
   * @param sheet             the sheet for which the column shall be adjusted
   * @param columnIndex       the index of the column to adjust
   * @param widthInCharacters the designated width of the column in number of characters
   * @throws IllegalArgumentException if the number of characters > 255
   * @since 1.5.0
   */
  public static void setColumnWidth(Sheet sheet, int columnIndex, int widthInCharacters)
      throws IllegalArgumentException {
    Objects.requireNonNull(sheet);
    if (widthInCharacters > COLUMN_MAX_WIDTH) {
      throw new IllegalArgumentException(
          "Column width must be less than %s characters. Provided: %s".formatted(COLUMN_MAX_WIDTH, widthInCharacters));
    }
    sheet.setColumnWidth(columnIndex, widthInCharacters * 256);
  }


  public static CellStyle createBoldCellStyle(Workbook workbook) {
    CellStyle boldStyle = workbook.createCellStyle();
    Font fontBold = workbook.createFont();
    fontBold.setBold(true);
    boldStyle.setFont(fontBold);

    return boldStyle;
  }

  public static CellStyle createReadOnlyCellStyle(Workbook workbook) {
    CellStyle readOnlyStyle = workbook.createCellStyle();
    readOnlyStyle.setFillForegroundColor(new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
    readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    XSSFFont font = (XSSFFont) workbook.createFont();
    font.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
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
  protected static String toCamelCase(String input) {
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
    DataValidationHelper dataValidationHelper = sheet.getDataValidationHelper();
    DataValidationConstraint formulaListConstraint = dataValidationHelper
        .createFormulaListConstraint(allowedValues.getNameName());
    DataValidation validation = dataValidationHelper.createValidation(formulaListConstraint,
        validatedCells);
    validation.setSuppressDropDownArrow(true); // shows dropdown if true
    validation.setShowErrorBox(true);
    validation.createErrorBox("Invalid choice", "Please select a value from the dropdown list.");
    sheet.addValidationData(validation);
  }

  public static void addInputHelper(Sheet sheet, int startColIdx, int startRowIdx,
      int stopColIdx, int stopRowIdx, String title, String content) {
    CellRangeAddressList validatedCells = new CellRangeAddressList(startRowIdx,
        stopRowIdx,
        startColIdx,
        stopColIdx);
    DataValidationHelper dataValidationHelper = sheet.getDataValidationHelper();
    DataValidationConstraint alwaysTrue = dataValidationHelper.createCustomConstraint("TRUE");
    DataValidation validation = dataValidationHelper.createValidation(alwaysTrue,
        validatedCells);
    validation.setSuppressDropDownArrow(true); // shows dropdown if true
    validation.setShowPromptBox(true);
    validation.createPromptBox(title, content);
    sheet.addValidationData(validation);
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
