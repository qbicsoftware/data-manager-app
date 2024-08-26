package life.qbic.datamanager.parser.xlsx;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import life.qbic.datamanager.parser.MetadataParser;
import life.qbic.datamanager.parser.ParsingResult;
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

  private boolean headerToLowerCase = false;

  private boolean firstRowIsHeader = true;

  private XLSXParser() {
  }

  private XLSXParser(boolean headerToLowerCase, boolean firstRowIsHeader) {
    this.headerToLowerCase = headerToLowerCase;
    this.firstRowIsHeader = firstRowIsHeader;
  }

  public static XLSXParser create(boolean firstRowIsHeader) {
    return new XLSXParser(false, firstRowIsHeader);
  }

  public static XLSXParser createWithHeaderToLowerCase(boolean firstRowIsHeader) {
    return new XLSXParser(true, firstRowIsHeader);
  }

  private static String readCellAsString(Cell cell) {
    return switch (cell.getCellType()) {
      case _NONE, ERROR, BOOLEAN, FORMULA -> null;
      case BLANK -> "";
      case NUMERIC -> String.valueOf(cell.getNumericCellValue());
      case STRING -> cell.getStringCellValue();
    };
  }

  @Override
  public ParsingResult parse(InputStream inputStream) {
    XSSFWorkbook workbook = null;
    try {
      workbook = new XSSFWorkbook(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    XSSFSheet metadataSheet = workbook.getSheetAt(0);
    XSSFRow headerRow = metadataSheet.getRow(0);

    if (isNull(headerRow)) {
      throw new MetadataParser.UnknownPropertiesException(
          "No properties have been found: did you provide a header row?");
    }
    List<List<String>> rows = new ArrayList<>();
    Map<String, Integer> columns = new HashMap<>();
    Iterator<Cell> cellIterator = headerRow.cellIterator();
    //do not use while loop with the cell iterator!
    //It will not return null but the same cell over and over if hasNext is not checked.

    Cell cell;
    while (cellIterator.hasNext()) {
      cell = cellIterator.next();
      var cellValue =
          headerToLowerCase ? readCellAsString(cell).toLowerCase() : readCellAsString(cell);
      columns.put(cellValue, cell.getColumnIndex());
    }

    Iterator<Row> rowIterator = metadataSheet.rowIterator();
    Row row;
    if (firstRowIsHeader) {
      rowIterator.next(); // skip the first entry, since it contains the header
    }
    while (rowIterator.hasNext()) {
      row = rowIterator.next();
      String[] rowData = new String[columns.size()];
      for (Entry<String, Integer> columnEntry : columns.entrySet()) {
        rowData[columnEntry.getValue()] = readCellAsString(row.getCell(columnEntry.getValue()));
      }
      rows.add(Arrays.stream(rowData).toList());
    }

    return new ParsingResult(columns, rows);
  }
}
