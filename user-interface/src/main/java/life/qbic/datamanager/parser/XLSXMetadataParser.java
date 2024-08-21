package life.qbic.datamanager.views.projects.project.measurements;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import life.qbic.datamanager.parser.MetadataParser;
import life.qbic.projectmanagement.application.measurement.MeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementValidationService.Domain;
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
public class XLSXMetadataParser implements
    MetadataParser<MeasurementMetadata> {

  private final MeasurementValidationService validationService;

  public XLSXMetadataParser(MeasurementValidationService validationService) {
    this.validationService = validationService;
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
  public List<MeasurementMetadata> parse(InputStream inputStream) {

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
    Map<String, Integer> columns = new HashMap<>();
    Iterator<Cell> cellIterator = headerRow.cellIterator();
    //do not use while loop with the cell iterator!
    //It will not return null but the same cell over and over if hasNext is not checked.
    for (Cell cell = cellIterator.next(); cellIterator.hasNext(); cell = cellIterator.next()) {
      var valueAsString = readCellAsString(cell);
      columns.put(valueAsString, cell.getColumnIndex());
    }
    Row row;
    Domain domain = validationService.inferDomainByPropertyTypes(columns.keySet())
        .orElseThrow(() -> new MetadataParser.UnknownDomainException(
            "Metadata could not be mapped to any known domain."));
    var metadataList = new ArrayList<MeasurementMetadata>();
    while ((row = metadataSheet.rowIterator().next()) != null) {
      MeasurementMetadataRow measurementMetadataRow = new MeasurementMetadataRow();
      for (Entry<String, Integer> columnEntry : columns.entrySet()) {
        measurementMetadataRow.put(columnEntry.getKey(),
            readCellAsString(row.getCell(columnEntry.getValue())));
      }
      MeasurementMetadata metadata = switch (domain) {
        case NGS -> parseNgs(columns, metadataSheet);
        case PROTEOMICS -> parseProteomics(columns, metadataSheet);
      };
      metadataList.add(metadata);
    }
    return Collections.unmodifiableList(metadataList);
  }

  private MeasurementMetadata parseProteomics(Map<String, Integer> columns,
      XSSFSheet metadataSheet) {
    throw new RuntimeException("Not implemented");
  }

  private MeasurementMetadata parseNgs(Map<String, Integer> columns, XSSFSheet metadataSheet) {
    throw new RuntimeException("Not implemented");
  }

  public static class MeasurementMetadataRow {

    private final HashMap<String, String> values = new HashMap<>();

    public void put(String column, String value) {
      values.put(column, value);
    }

    public String valueForColumn(String column) {
      return values.get(column);
    }

    @Override
    public final boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof MeasurementMetadataRow that)) {
        return false;
      }

      return values.equals(that.values);
    }

    @Override
    public int hashCode() {
      return values.hashCode();
    }
  }
}
