package life.qbic.datamanager.views.projects.project.measurements.download;

import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.addDataValidation;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.lockSheet;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;
import life.qbic.datamanager.views.projects.project.measurements.ProteomicsMeasurementEntry;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.ProteomicsMeasurementMetadata;
import life.qbic.projectmanagement.application.measurement.validation.MeasurementProteomicsValidator.DigestionMethod;
import life.qbic.projectmanagement.domain.model.measurement.ProteomicsMeasurement;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** <b>Proteomics Measurement Content Provider</b>
 * <p>
 * Implementation of the {@link DownloadContentProvider} providing the content and file name for any files created
 * from {@link ProteomicsMeasurement}
 * and {@link ProteomicsMeasurementMetadata}
 * </p>
 */
public class ProteomicsMeasurementContentProvider implements DownloadContentProvider {

  private static final String FILE_NAME_SUFFIX = "proteomics_measurements.xlsx";
  private static final Logger log = logger(ProteomicsMeasurementContentProvider.class);
  private static final byte[] DARK_GREY = {119, 119, 119};
  private static final byte[] LIGHT_GREY = {(byte) 220, (byte) 220, (byte) 220};
  private final List<ProteomicsMeasurementEntry> measurements = new LinkedList<>();
  private static final String DEFAULT_FILE_NAME_PREFIX = "QBiC";
  private String fileNamePrefix = DEFAULT_FILE_NAME_PREFIX;
  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;


  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= 18; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  private static void createMeasurementEntry(ProteomicsMeasurementEntry pxpEntry, Row entryRow,
      CellStyle readOnlyStyle) {
    for (ProteomicsMeasurementColumns measurementColumn : ProteomicsMeasurementColumns.values()) {
      var value = switch (measurementColumn) {
        case MEASUREMENT_ID -> pxpEntry.measurementCode();
        case SAMPLE_ID -> pxpEntry.sampleInformation().sampleId();
        case SAMPLE_NAME -> pxpEntry.sampleInformation().sampleName();
        case POOL_GROUP -> pxpEntry.samplePoolGroup();
        case ORGANISATION_ID -> pxpEntry.organisationId();
        case ORGANISATION_NAME -> pxpEntry.organisationName();
        case FACILITY -> pxpEntry.facility();
        case MS_DEVICE -> pxpEntry.msDeviceCURIE();
        case MS_DEVICE_NAME -> pxpEntry.msDeviceName();
        case CYCLE_FRACTION_NAME -> pxpEntry.fractionName();
        case DIGESTION_METHOD -> pxpEntry.digestionMethod();
        case DIGESTION_ENZYME -> pxpEntry.digestionEnzyme();
        case ENRICHMENT_METHOD -> pxpEntry.enrichmentMethod();
        case INJECTION_VOLUME -> pxpEntry.injectionVolume();
        case LC_COLUMN -> pxpEntry.lcColumn();
        case LCMS_METHOD -> pxpEntry.lcmsMethod();
        case LABELING_TYPE -> pxpEntry.labelingType();
        case LABEL -> pxpEntry.label();
        case COMMENT -> pxpEntry.comment();
      };
      var cell = getOrCreateCell(entryRow, measurementColumn.columnIndex());
      cell.setCellValue(value);
      if (measurementColumn.readOnly()) {
        cell.setCellStyle(readOnlyStyle);
      }
    }
  }

  public void setMeasurements(List<ProteomicsMeasurementEntry> measurements, String fileNamePrefix) {
    this.measurements.clear();
    this.measurements.addAll(measurements);
    this.fileNamePrefix = fileNamePrefix;
  }

  @Override
  public byte[] getContent() {
    if (measurements.isEmpty()) {
      return new byte[0];
    }

    ByteArrayOutputStream byteArrayOutputStream;

    try (Workbook workbook = new XSSFWorkbook()) {

      CellStyle readOnlyHeaderStyle = workbook.createCellStyle();
      readOnlyHeaderStyle.setFillForegroundColor(
          new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
      readOnlyHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      XSSFFont fontHeader = (XSSFFont) workbook.createFont();
      fontHeader.setBold(true);
      fontHeader.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
      readOnlyHeaderStyle.setFont(fontHeader);

      CellStyle boldStyle = workbook.createCellStyle();
      Font fontBold = workbook.createFont();
      fontBold.setBold(true);
      boldStyle.setFont(fontBold);

      CellStyle readOnlyStyle = workbook.createCellStyle();
      readOnlyStyle.setFillForegroundColor(new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
      readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      XSSFFont font = (XSSFFont) workbook.createFont();
      font.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
      readOnlyStyle.setFont(font);

      Sheet sheet = workbook.createSheet("Proteomics Measurement Metadata");
      Row header = getOrCreateRow(sheet, 0);
      for (ProteomicsMeasurementColumns measurementColumn : ProteomicsMeasurementColumns.values()) {
        var cell = getOrCreateCell(header, measurementColumn.columnIndex());
        cell.setCellValue(measurementColumn.headerName());
        if (measurementColumn.readOnly()) {
          cell.setCellStyle(readOnlyHeaderStyle);
        } else {
          cell.setCellStyle(boldStyle);
        }
      }

      var startIndex = 1; // start in row number 2 with index 1 skipping the header in the first row
      var rowIndex = startIndex;

      for (ProteomicsMeasurementEntry pxpEntry : measurements) {
        Row entry = getOrCreateRow(sheet, rowIndex);
        createMeasurementEntry(pxpEntry, entry, readOnlyStyle);
        rowIndex++;
      }
      var generatedRowCount = rowIndex - startIndex;
      assert generatedRowCount == measurements.size() : "all measurements have a corresponding row";

      // make sure to create the visible sheet first
      Sheet hiddenSheet = workbook.createSheet("hidden");
      Name digestionMethodArea = createOptionArea(hiddenSheet, "Digestion Method",
          DigestionMethod.getOptions());

      addDataValidation(sheet,
          ProteomicsMeasurementColumns.DIGESTION_METHOD.columnIndex(), startIndex,
          ProteomicsMeasurementColumns.DIGESTION_METHOD.columnIndex(),
          DEFAULT_GENERATED_ROW_COUNT - 1,
          digestionMethodArea);

      setAutoWidth(sheet);
      workbook.setActiveSheet(0);

      lockSheet(hiddenSheet);
      hideSheet(workbook, hiddenSheet);

      byteArrayOutputStream = new ByteArrayOutputStream();
      workbook.write(byteArrayOutputStream);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new ApplicationException(ErrorCode.GENERAL, null);
    }

    return byteArrayOutputStream.toByteArray();
  }

  enum ProteomicsMeasurementColumns {

    MEASUREMENT_ID("Measurement ID", 0, true),
    SAMPLE_ID("QBiC Sample Id", 1, true),
    SAMPLE_NAME(
        "Sample Name", 2, true),
    POOL_GROUP("Sample Pool Group", 3, true),
    ORGANISATION_ID("Organisation ID", 4, false),
    ORGANISATION_NAME("Organisation Name", 5, true),
    FACILITY("Facility", 6, false),
    MS_DEVICE("MS Device", 7, false),
    MS_DEVICE_NAME("MS Device Name", 8, true),
    CYCLE_FRACTION_NAME("Cycle/Fraction Name", 9, false),
    DIGESTION_METHOD("Digestion Method", 10, false),
    DIGESTION_ENZYME("Digestion Enzyme", 11, false),
    ENRICHMENT_METHOD("Enrichment Method", 12, false),
    INJECTION_VOLUME("Injection Volume (µL)", 13, false),
    LC_COLUMN("LC Column", 14, false),
    LCMS_METHOD("LCMS Method", 15, false),
    LABELING_TYPE("Labeling Type", 16, false),
    LABEL("Label", 17, false),
    COMMENT("Comment", 18, false),
    ;
    private final String headerName;
    private final int columnIndex;
    private final boolean readOnly;

    ProteomicsMeasurementColumns(String headerName, int columnIndex, boolean readOnly) {
      this.headerName = headerName;
      this.columnIndex = columnIndex;
      this.readOnly = readOnly;
    }

    public String headerName() {
      return headerName;
    }

    public int columnIndex() {
      return columnIndex;
    }

    public boolean readOnly() {
      return readOnly;
    }
  }

  @Override
  public String getFileName() {
    return String.join("_", fileNamePrefix, FILE_NAME_SUFFIX);
  }
}
