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
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public class XLSXParser implements MetadataParser {

  public XLSXParser() {
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
      columns.put(readCellAsString(cell), cell.getColumnIndex());
    }


    Iterator<Row> rowIterator = metadataSheet.rowIterator();
    Row row;
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
