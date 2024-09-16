package life.qbic.datamanager.views.projects.project.measurements.download;

import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.lockSheet;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.spreadsheet.XLSXTemplateHelper;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;
import life.qbic.datamanager.views.projects.project.measurements.NGSMeasurementEntry;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <b>NGS Measurement Content Provider</b>
 * <p>
 * Implementation of the {@link DownloadContentProvider} providing the content and file name for any
 * files created from {@link NGSMeasurement} and {@link NGSMeasurementMetadata}
 * </p>
 */
public class NGSMeasurementContentProvider implements DownloadContentProvider {

  private static final String FILE_NAME_SUFFIX = "ngs_measurements.xlsx";
  private static final Logger log = logger(NGSMeasurementContentProvider.class);
  private static final byte[] DARK_GREY = {119, 119, 119};
  private static final byte[] LIGHT_GREY = {(byte) 220, (byte) 220, (byte) 220};
  private static CellStyle readOnlyCellStyle;
  private static CellStyle readOnlyHeaderStyle;
  private static CellStyle boldStyle;
  private final List<NGSMeasurementEntry> measurements = new LinkedList<>();
  private static final String DEFAULT_FILE_NAME_PREFIX = "QBiC";
  private String fileNamePrefix = DEFAULT_FILE_NAME_PREFIX;
  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;

  private enum SequencingReadType {
    SINGLE_END("single-end"),
    PAIRED_END("paired-end");
    private final String presentationString;

    SequencingReadType(String presentationString) {
      this.presentationString = presentationString;
    }

    static List<String> getOptions() {
      return Arrays.stream(values()).map(it -> it.presentationString).toList();
    }
  }

  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= NGSMeasurementColumns.values().length; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  private static void formatHeader(Row header) {
    for (NGSMeasurementColumns value : NGSMeasurementColumns.values()) {
      var cell = header.createCell(value.columnIndex());
      cell.setCellValue(value.headerName());
      setHeaderStyle(cell, value.readOnly());
    }
  }

  private static void setHeaderStyle(Cell cell, boolean isReadOnly) {
    if (isReadOnly) {
      cell.setCellStyle(readOnlyHeaderStyle);
    } else {
      cell.setCellStyle(boldStyle);
    }
  }

  private static void setCellStyle(Cell cell, boolean isReadOnly) {
    if (isReadOnly) {
      cell.setCellStyle(readOnlyCellStyle);
    }
  }

  private static void writeMeasurementIntoRow(NGSMeasurementEntry ngsMeasurementEntry,
      Row entryRow) {
    var measureCol = getOrCreateCell(entryRow, NGSMeasurementColumns.MEASUREMENT_ID.columnIndex());
    measureCol.setCellValue(ngsMeasurementEntry.measurementCode());
    setCellStyle(measureCol, NGSMeasurementColumns.MEASUREMENT_ID.readOnly());

    var sampleIdCol = getOrCreateCell(entryRow, NGSMeasurementColumns.SAMPLE_ID.columnIndex());
    sampleIdCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleId());
    setCellStyle(sampleIdCol, NGSMeasurementColumns.SAMPLE_ID.readOnly());

    var sampleNameCol = getOrCreateCell(entryRow, NGSMeasurementColumns.SAMPLE_NAME.columnIndex());
    sampleNameCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleName());
    setCellStyle(sampleNameCol, NGSMeasurementColumns.SAMPLE_NAME.readOnly());

    var orgIdCol = getOrCreateCell(entryRow, NGSMeasurementColumns.ORGANISATION_ID.columnIndex());
    orgIdCol.setCellValue(ngsMeasurementEntry.organisationId());
    setCellStyle(orgIdCol, NGSMeasurementColumns.ORGANISATION_ID.readOnly());

    var organisationNameCol = getOrCreateCell(entryRow,
        NGSMeasurementColumns.ORGANISATION_NAME.columnIndex());

    organisationNameCol.setCellValue(ngsMeasurementEntry.organisationName());
    setCellStyle(organisationNameCol, NGSMeasurementColumns.ORGANISATION_NAME.readOnly());

    var facilityCol = getOrCreateCell(entryRow, NGSMeasurementColumns.FACILITY.columnIndex());
    facilityCol.setCellValue(ngsMeasurementEntry.facility());
    setCellStyle(facilityCol, NGSMeasurementColumns.FACILITY.readOnly);
    var instrumentCol = getOrCreateCell(entryRow, NGSMeasurementColumns.INSTRUMENT.columnIndex());
    instrumentCol.setCellValue(ngsMeasurementEntry.instrumentCURI());
    setCellStyle(instrumentCol, NGSMeasurementColumns.INSTRUMENT.readOnly());

    var instrumentNameCol = getOrCreateCell(entryRow,
        NGSMeasurementColumns.INSTRUMENT_NAME.columnIndex());
    instrumentNameCol.setCellValue(ngsMeasurementEntry.instrumentName());
    setCellStyle(instrumentNameCol, NGSMeasurementColumns.INSTRUMENT_NAME.readOnly());

    var readTypeCol = getOrCreateCell(entryRow,
        NGSMeasurementColumns.SEQUENCING_READ_TYPE.columnIndex());
    readTypeCol.setCellValue(ngsMeasurementEntry.readType());
    setCellStyle(readTypeCol, NGSMeasurementColumns.SEQUENCING_READ_TYPE.readOnly());

    var libraryKitCol = getOrCreateCell(entryRow, NGSMeasurementColumns.LIBRARY_KIT.columnIndex());
    libraryKitCol.setCellValue(ngsMeasurementEntry.libraryKit());
    setCellStyle(libraryKitCol, NGSMeasurementColumns.LIBRARY_KIT.readOnly());

    var flowCellCol = getOrCreateCell(entryRow, NGSMeasurementColumns.FLOW_CELL.columnIndex());
    flowCellCol.setCellValue(ngsMeasurementEntry.flowCell());
    setCellStyle(flowCellCol, NGSMeasurementColumns.FLOW_CELL.readOnly());

    var runProtocolCol = getOrCreateCell(entryRow,
        NGSMeasurementColumns.SEQUENCING_RUN_PROTOCOL.columnIndex());
    runProtocolCol.setCellValue(ngsMeasurementEntry.runProtocol());
    setCellStyle(runProtocolCol, NGSMeasurementColumns.SEQUENCING_RUN_PROTOCOL.readOnly());

    var poolGroupCol = getOrCreateCell(entryRow, NGSMeasurementColumns.POOL_GROUP.columnIndex());
    poolGroupCol.setCellValue(ngsMeasurementEntry.samplePoolGroup());
    setCellStyle(poolGroupCol, NGSMeasurementColumns.POOL_GROUP.readOnly());

    var indexI7Col = getOrCreateCell(entryRow, NGSMeasurementColumns.INDEX_I7.columnIndex());
    indexI7Col.setCellValue(ngsMeasurementEntry.indexI7());
    setCellStyle(indexI7Col, NGSMeasurementColumns.INDEX_I7.readOnly());

    var indexI5Col = getOrCreateCell(entryRow, NGSMeasurementColumns.INDEX_I5.columnIndex());
    indexI5Col.setCellValue(ngsMeasurementEntry.indexI5());
    setCellStyle(indexI5Col, NGSMeasurementColumns.INDEX_I5.readOnly());

    var commentCol = getOrCreateCell(entryRow, NGSMeasurementColumns.COMMENT.columnIndex());
    commentCol.setCellValue(ngsMeasurementEntry.comment());
    setCellStyle(commentCol, NGSMeasurementColumns.COMMENT.readOnly());

    for (NGSMeasurementColumns measurementColumn : NGSMeasurementColumns.values()) {
      var value = switch (measurementColumn) {
        case MEASUREMENT_ID -> ngsMeasurementEntry.measurementCode();
        case SAMPLE_ID -> ngsMeasurementEntry.sampleInformation().sampleId();
        case SAMPLE_NAME -> ngsMeasurementEntry.sampleInformation().sampleName();
        case POOL_GROUP -> ngsMeasurementEntry.samplePoolGroup();
        case ORGANISATION_ID -> ngsMeasurementEntry.organisationId();
        case ORGANISATION_NAME -> ngsMeasurementEntry.organisationName();
        case FACILITY -> ngsMeasurementEntry.facility();
        case INSTRUMENT -> ngsMeasurementEntry.instrumentCURI();
        case INSTRUMENT_NAME -> ngsMeasurementEntry.instrumentName();
        case SEQUENCING_READ_TYPE -> ngsMeasurementEntry.readType();
        case LIBRARY_KIT -> ngsMeasurementEntry.libraryKit();
        case FLOW_CELL -> ngsMeasurementEntry.flowCell();
        case SEQUENCING_RUN_PROTOCOL -> ngsMeasurementEntry.runProtocol();
        case INDEX_I7 -> ngsMeasurementEntry.indexI7();
        case INDEX_I5 -> ngsMeasurementEntry.indexI5();
        case COMMENT -> ngsMeasurementEntry.comment();
      };
      var cell = getOrCreateCell(entryRow, measurementColumn.columnIndex());
      cell.setCellValue(value);
      if (measurementColumn.readOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }
    
    
  }

  public void setMeasurements(List<NGSMeasurementEntry> measurements, String fileNamePrefix) {
    this.measurements.clear();
    this.measurements.addAll(measurements);
    this.fileNamePrefix = fileNamePrefix.trim();
  }

  private void defineBoldStyle(Workbook workbook) {
    boldStyle = workbook.createCellStyle();
    Font fontBold = workbook.createFont();
    fontBold.setBold(true);
    boldStyle.setFont(fontBold);
  }

  private void defineReadOnlyCellStyle(Workbook workbook) {
    readOnlyCellStyle = workbook.createCellStyle();
    readOnlyCellStyle.setFillForegroundColor(
        new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
    readOnlyCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    XSSFFont font = (XSSFFont) workbook.createFont();
    font.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
    readOnlyCellStyle.setFont(font);
  }

  private void defineReadOnlyHeaderStyle(Workbook workbook) {
    readOnlyHeaderStyle = workbook.createCellStyle();
    readOnlyHeaderStyle.setFillForegroundColor(
        new XSSFColor(LIGHT_GREY, new DefaultIndexedColorMap()));
    readOnlyHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    XSSFFont fontHeader = (XSSFFont) workbook.createFont();
    fontHeader.setBold(true);
    fontHeader.setColor(new XSSFColor(DARK_GREY, new DefaultIndexedColorMap()));
    readOnlyHeaderStyle.setFont(fontHeader);
  }


  @Override
  public byte[] getContent() {
    if (measurements.isEmpty()) {
      return new byte[0];
    }

    ByteArrayOutputStream byteArrayOutputStream;

    try (Workbook workbook = new XSSFWorkbook()) {
      defineReadOnlyHeaderStyle(workbook);
      defineReadOnlyCellStyle(workbook);
      defineBoldStyle(workbook);

      Sheet hiddenSheet = workbook.createSheet("hidden");

      Name sequencingReadTypeArea = createOptionArea(hiddenSheet,
          "Sequencing read type", SequencingReadType.getOptions());

      Sheet sheet = workbook.createSheet("NGS Measurement Metadata");

      Row header = getOrCreateRow(sheet, 0);
      for (NGSMeasurementColumns value : NGSMeasurementColumns.values()) {
        var cell = header.createCell(value.columnIndex());
        cell.setCellValue(value.headerName());
        setHeaderStyle(cell, value.readOnly());
      }

      var startIndex = 1; // start in row number 2 with index 1 as the header row has number 1 index 0
      int rowIndex = startIndex;
      for (NGSMeasurementEntry measurement : measurements) {
        Row row = getOrCreateRow(sheet, rowIndex);
        writeMeasurementIntoRow(measurement, row);
        rowIndex++;
      }

      var generatedRowCount = rowIndex - startIndex;
      assert generatedRowCount == measurements.size() : "all measurements have a corresponding row";

      XLSXTemplateHelper.addDataValidation(sheet,
          NGSMeasurementColumns.SEQUENCING_READ_TYPE.columnIndex(),
          startIndex,
          NGSMeasurementColumns.SEQUENCING_READ_TYPE.columnIndex(),
          DEFAULT_GENERATED_ROW_COUNT - 1,
          sequencingReadTypeArea);

      setAutoWidth(sheet);
      workbook.setSheetOrder(sheet.getSheetName(), 0);
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

  private static Row getOrCreateRow(Sheet sheet, int index) {
    return Optional.ofNullable(sheet.getRow(index))
        .orElse(sheet.createRow(index));
  }

  private static Cell getOrCreateCell(Row row, int colIndex) {
    return Optional.ofNullable(row.getCell(colIndex, MissingCellPolicy.RETURN_BLANK_AS_NULL))
        .orElse(row.createCell(colIndex));
  }

  @Override
  public String getFileName() {
    return String.join("_", fileNamePrefix, FILE_NAME_SUFFIX);
  }

  /**
   * <b>NGS Measurement Columns</b>
   *
   * <p>Enumeration of the columns shown in the file used for NGS measurement registration and edit
   * in the context of measurement file based upload.
   * Provides the name of the header column, the column index and if the column should be set to
   * readOnly in the generated sheet
   * </p>
   */
  enum NGSMeasurementColumns {

    MEASUREMENT_ID("Measurement ID", 0,
        true),
    SAMPLE_ID("QBiC Sample Id", 1,
        true),
    SAMPLE_NAME(
        "Sample Name", 2,
        true),
    POOL_GROUP("Sample Pool Group", 3,
        true),
    ORGANISATION_ID("Organisation ID", 4,
        false),
    ORGANISATION_NAME("Organisation Name", 5,
        true),
    FACILITY("Facility", 6,
        false),
    INSTRUMENT("Instrument", 7,
        false),
    INSTRUMENT_NAME("Instrument Name", 8,
        true),
    SEQUENCING_READ_TYPE("Sequencing Read Type", 9,
        false),
    LIBRARY_KIT("Library Kit", 10,
        false),
    FLOW_CELL("Flow Cell", 11,
        false),
    SEQUENCING_RUN_PROTOCOL("Sequencing Run Protocol", 12,
        false),
    INDEX_I7("Index i7", 13,
        false),
    INDEX_I5("Index i5", 14,
        false),
    COMMENT("Comment", 15,
        false),
    ;

    private final String headerName;
    private final int columnIndex;
    private final boolean readOnly;

    static int maxColumnIndex() {
      return Arrays.stream(values())
          .mapToInt(NGSMeasurementColumns::columnIndex)
          .max().orElse(0);
    }

    /**
     * @param headerName    the name in the header
     * @param columnIndex   the index of the column this property is in
     * @param readOnly      is the property read only
     */
    NGSMeasurementColumns(String headerName, int columnIndex, boolean readOnly) {
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
}
