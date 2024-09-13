package life.qbic.datamanager.views.projects.project.measurements.download;

import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.spreadsheet.XLSXTemplateHelper.lockSheet;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
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

  private static void writeMeasurementIntoRow(NGSMeasurementEntry ngsMeasurementEntry, Row entry) {
    var measureCol = getOrCreateCell(entry, NGSMeasurementColumns.MEASUREMENTCODE.columnIndex());
    measureCol.setCellValue(ngsMeasurementEntry.measurementCode());
    setCellStyle(measureCol, NGSMeasurementColumns.MEASUREMENTCODE.readOnly());

    var sampleIdCol = getOrCreateCell(entry, NGSMeasurementColumns.SAMPLEID.columnIndex());
    sampleIdCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleId());
    setCellStyle(sampleIdCol, NGSMeasurementColumns.SAMPLEID.readOnly());

    var sampleNameCol = getOrCreateCell(entry, NGSMeasurementColumns.SAMPLENAME.columnIndex());
    sampleNameCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleName());
    setCellStyle(sampleNameCol, NGSMeasurementColumns.SAMPLENAME.readOnly());

    var orgIdCol = getOrCreateCell(entry, NGSMeasurementColumns.ORGANISATIONID.columnIndex());
    orgIdCol.setCellValue(ngsMeasurementEntry.organisationId());
    setCellStyle(orgIdCol, NGSMeasurementColumns.ORGANISATIONID.readOnly());

    var organisationNameCol = getOrCreateCell(entry,
        NGSMeasurementColumns.ORGANISATIONNAME.columnIndex());

    organisationNameCol.setCellValue(ngsMeasurementEntry.organisationName());
    setCellStyle(organisationNameCol, NGSMeasurementColumns.ORGANISATIONNAME.readOnly());

    var facilityCol = getOrCreateCell(entry, NGSMeasurementColumns.FACILITY.columnIndex());
    facilityCol.setCellValue(ngsMeasurementEntry.facility());
    setCellStyle(facilityCol, NGSMeasurementColumns.FACILITY.readOnly);
    var instrumentCol = getOrCreateCell(entry, NGSMeasurementColumns.INSTRUMENT.columnIndex());
    instrumentCol.setCellValue(ngsMeasurementEntry.instrumentCURI());
    setCellStyle(instrumentCol, NGSMeasurementColumns.INSTRUMENT.readOnly());

    var instrumentNameCol = getOrCreateCell(entry,
        NGSMeasurementColumns.INSTRUMENTNAME.columnIndex());
    instrumentNameCol.setCellValue(ngsMeasurementEntry.instrumentName());
    setCellStyle(instrumentNameCol, NGSMeasurementColumns.INSTRUMENTNAME.readOnly());

    var readTypeCol = getOrCreateCell(entry,
        NGSMeasurementColumns.SEQUENCINGREADTYPE.columnIndex());
    readTypeCol.setCellValue(ngsMeasurementEntry.readType());
    setCellStyle(readTypeCol, NGSMeasurementColumns.SEQUENCINGREADTYPE.readOnly());

    var libraryKitCol = getOrCreateCell(entry, NGSMeasurementColumns.LIBRARYKIT.columnIndex());
    libraryKitCol.setCellValue(ngsMeasurementEntry.libraryKit());
    setCellStyle(libraryKitCol, NGSMeasurementColumns.LIBRARYKIT.readOnly());

    var flowCellCol = getOrCreateCell(entry, NGSMeasurementColumns.FLOWCELL.columnIndex());
    flowCellCol.setCellValue(ngsMeasurementEntry.flowCell());
    setCellStyle(flowCellCol, NGSMeasurementColumns.FLOWCELL.readOnly());

    var runProtocolCol = getOrCreateCell(entry, NGSMeasurementColumns.RUNPROTOCOL.columnIndex());
    runProtocolCol.setCellValue(ngsMeasurementEntry.runProtocol());
    setCellStyle(runProtocolCol, NGSMeasurementColumns.RUNPROTOCOL.readOnly());

    var poolGroupCol = getOrCreateCell(entry, NGSMeasurementColumns.POOLGROUP.columnIndex());
    poolGroupCol.setCellValue(ngsMeasurementEntry.samplePoolGroup());
    setCellStyle(poolGroupCol, NGSMeasurementColumns.POOLGROUP.readOnly());

    var indexI7Col = getOrCreateCell(entry, NGSMeasurementColumns.INDEXI7.columnIndex());
    indexI7Col.setCellValue(ngsMeasurementEntry.indexI7());
    setCellStyle(indexI7Col, NGSMeasurementColumns.INDEXI7.readOnly());

    var indexI5Col = getOrCreateCell(entry, NGSMeasurementColumns.INDEXI5.columnIndex());
    indexI5Col.setCellValue(ngsMeasurementEntry.indexI5());
    setCellStyle(indexI5Col, NGSMeasurementColumns.INDEXI5.readOnly());

    var commentCol = getOrCreateCell(entry, NGSMeasurementColumns.COMMENT.columnIndex());
    commentCol.setCellValue(ngsMeasurementEntry.comment());
    setCellStyle(commentCol, NGSMeasurementColumns.COMMENT.readOnly());
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
          NGSMeasurementColumns.SEQUENCINGREADTYPE.columnIndex(),
          startIndex,
          NGSMeasurementColumns.SEQUENCINGREADTYPE.columnIndex(),
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

    MEASUREMENTCODE("Measurement ID", 0,
        true),
    SAMPLEID("QBiC Sample Id", 1,
        true),
    SAMPLENAME(
        "Sample Name", 2,
        true),
    POOLGROUP("Sample Pool Group", 3,
        true),
    ORGANISATIONID("Organisation ID", 4,
        false),
    ORGANISATIONNAME("Organisation Name", 5,
        true),
    FACILITY("Facility", 6,
        false),
    INSTRUMENT("Instrument", 7,
        false),
    INSTRUMENTNAME("Instrument Name", 8,
        true),
    SEQUENCINGREADTYPE("Sequencing Read Type", 9,
        false, List.of("single-end", "paired-end")),
    LIBRARYKIT("Library Kit", 10,
        false),
    FLOWCELL("Flow Cell", 11,
        false),
    RUNPROTOCOL("Sequencing Run Protocol", 12,
        false),
    INDEXI7("Index i7", 13,
        false),
    INDEXI5("Index i5", 14,
        false),
    COMMENT("Comment", 15,
        false),
    ;

    private final String headerName;
    private final int columnIndex;
    private final boolean readOnly;
    private final List<String> allowedValues;

    static int maxColumnIndex() {
      return Arrays.stream(values())
          .mapToInt(NGSMeasurementColumns::columnIndex)
          .max().orElse(0);
    }

    /**
     * @param headerName    the name in the header
     * @param columnIndex   the index of the column this property is in
     * @param readOnly      is the property read only
     * @param allowedValues a list of allowed values; null if not applicable
     */
    NGSMeasurementColumns(String headerName, int columnIndex, boolean readOnly,
        List<String> allowedValues) {
      this.headerName = headerName;
      this.columnIndex = columnIndex;
      this.readOnly = readOnly;
      this.allowedValues = allowedValues;
    }

    /**
     * @param headerName  the name in the header
     * @param columnIndex the index of the column this property is in
     * @param readOnly    is the property read only
     */
    NGSMeasurementColumns(String headerName, int columnIndex, boolean readOnly) {
      this(headerName, columnIndex, readOnly, null);
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

    public List<String> allowedValues() {
      return Optional.ofNullable(allowedValues).orElse(Collections.emptyList());
    }
  }
}
