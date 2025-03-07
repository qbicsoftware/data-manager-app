package life.qbic.datamanager.files.parsing.xlsx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.Objects.isNull;
import java.util.Optional;
import life.qbic.datamanager.files.parsing.MetadataParser;
import life.qbic.datamanager.files.parsing.ParsingResult;
import life.qbic.datamanager.files.parsing.Sanitizer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <b>XLSX parser implementation of the {@link MetadataParser} interface.</b>
 * <p>
 * Parses information from content following the XLSX format specification:
 * <p>
 * <a
 * href="https://learn.microsoft.com/en-us/openspecs/office_standards/ms-xlsx/2c5dee00-eff2-4b22-92b6-0738acd4475e">
 * https://learn.microsoft.com/en-us/openspecs/office_standards/ms-xlsx/2c5dee00-eff2-4b22-92b6-0738acd4475e</a>
 *
 * @since 1.4.0
 */
public class XLSXParser implements MetadataParser {

  private XLSXParser() {
  }

  public static XLSXParser create() {
    return new XLSXParser();
  }

  /**
   * Reads the cell value as String. If the cell is `null`, or one of the following types is
   * present, the function will return an empty String:
   *
   * <ul>
   *   <li>_NONE</li>
   *   <li>ERROR</li>
   *   <li>BOOLEAN</li>
   *   <li>FORMULA</li>
   *   <li>BLANK</li>
   * </ul>
   *
   * @param cell the cell to extract the value from
   * @return the cell value in String representation
   * @since 1.4.0
   */
  private static String readCellAsString(Cell cell) {
    if (cell == null) {
      return "";
    }
    return switch (cell.getCellType()) {
      case _NONE, ERROR, FORMULA, BLANK -> "";
      case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
      case NUMERIC -> Double.toString(cell.getNumericCellValue());
      case STRING -> cell.getStringCellValue();
    };
  }

  @Override
  public ParsingResult parse(InputStream inputStream) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
      return parse(workbook);
    } catch (IOException e) {
      throw new ParsingException("Parsing failed", e);
    }
  }

  private ParsingResult parse(XSSFWorkbook workbook) {
    XSSFSheet metadataSheet = workbook.getSheetAt(0);
    XSSFRow headerRow = Optional.ofNullable(metadataSheet.getRow(0))
        .orElseThrow(() -> new ParsingException("No header row found"));

    if (isNull(headerRow)) {
      throw new MetadataParser.UnknownPropertiesException(
          "No properties have been found: did you provide a header row?");
    }
    List<ParsingResult.Row> rows = new ArrayList<>();
    Map<String, Integer> propertyToIndex = new HashMap<>();
    Iterator<Cell> cellIterator = headerRow.cellIterator();
    //do not use while loop with the cell iterator!
    //It will not return null but the same cell over and over if hasNext is not checked.

    Cell cell;
    while (cellIterator.hasNext()) {
      cell = cellIterator.next();
      var cellValue = Sanitizer.headerEncoder(readCellAsString(cell));
      if (propertyToIndex.containsKey(cellValue)) {
        throw new ParsingException("Duplicate column found: " + cellValue);
      }
      propertyToIndex.put(cellValue, cell.getColumnIndex());
    }

    Iterator<Row> rowIterator = metadataSheet.rowIterator();
    Row row;
    rowIterator.next(); // skip the first entry, since it contains the header

    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      String[] rowData = new String[propertyToIndex.size()];
      for (Entry<String, Integer> columnEntry : propertyToIndex.entrySet()) {
        rowData[columnEntry.getValue()] = readCellAsString(row.getCell(columnEntry.getValue()));
      }
      if (Sanitizer.containsInformation(rowData)) {
        rows.add(new ParsingResult.Row(Arrays.stream(rowData).toList()));
      }
    }

    return new ParsingResult(propertyToIndex, rows);
  }
}
