package life.qbic.datamanager.views.projects.project.measurements.download;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.views.general.download.DownloadContentProvider;
import life.qbic.datamanager.views.projects.project.measurements.NGSMeasurementEntry;
import life.qbic.logging.api.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** <b>NGS Measurement Content Provider</b>
 * <p>
 * Implementation of the {@link DownloadContentProvider} providing the content and file name for any files created
 * from {@link life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement}
 * and {@link life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata}
 * </p>
 */
public class NGSMeasurementContentProvider implements DownloadContentProvider {

  private static final String FILE_NAME = "ngs_measurements.xlsx";
  private static final Logger log = logger(NGSMeasurementContentProvider.class);
  private static final byte[] DARK_GREY = {119, 119, 119};
  private static final byte[] LIGHT_GREY = {(byte) 220, (byte) 220, (byte) 220};
  private static CellStyle readOnlyCellStyle;
  private static CellStyle readOnlyHeaderStyle;
  private static CellStyle boldStyle;
  private final List<NGSMeasurementEntry> measurements = new LinkedList<>();

  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= NGSMeasurementColumns.values().length; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  private static void formatHeader(Row header) {
    for (NGSMeasurementColumns value : NGSMeasurementColumns.values()) {
      var cell = header.createCell(value.columnNumber());
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

  private static void createMeasurementEntry(NGSMeasurementEntry ngsMeasurementEntry, Row entry) {
    var measureCol = entry.createCell(NGSMeasurementColumns.MEASUREMENTCODE.columnNumber());
    measureCol.setCellValue(ngsMeasurementEntry.measurementCode());
    setCellStyle(measureCol, NGSMeasurementColumns.MEASUREMENTCODE.readOnly());

    var sampleIdCol = entry.createCell(NGSMeasurementColumns.SAMPLEID.columnNumber());
    sampleIdCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleId());
    setCellStyle(sampleIdCol, NGSMeasurementColumns.SAMPLEID.readOnly());

    var sampleLabelCol = entry.createCell(NGSMeasurementColumns.SAMPLELABEL.columnNumber());
    sampleLabelCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleLabel());
    setCellStyle(sampleLabelCol, NGSMeasurementColumns.SAMPLELABEL.readOnly());

    var orgIdCol = entry.createCell(NGSMeasurementColumns.ORGANISATIONID.columnNumber());
    orgIdCol.setCellValue(ngsMeasurementEntry.organisationId());
    setCellStyle(orgIdCol, NGSMeasurementColumns.ORGANISATIONID.readOnly());

    var organisationNameCol = entry.createCell(
        NGSMeasurementColumns.ORGANISATIONNAME.columnNumber());
    organisationNameCol.setCellValue(ngsMeasurementEntry.organisationName());
    setCellStyle(organisationNameCol, NGSMeasurementColumns.ORGANISATIONNAME.readOnly());

    var facilityCol = entry.createCell(NGSMeasurementColumns.FACILITY.columnNumber());
    facilityCol.setCellValue(ngsMeasurementEntry.facility());
    setCellStyle(facilityCol, NGSMeasurementColumns.FACILITY.readOnly);
    var instrumentCol = entry.createCell(NGSMeasurementColumns.INSTRUMENT.columnNumber());
    instrumentCol.setCellValue(ngsMeasurementEntry.instrumentCURI());
    setCellStyle(instrumentCol, NGSMeasurementColumns.INSTRUMENT.readOnly());

    var instrumentNameCol = entry.createCell(NGSMeasurementColumns.INSTRUMENTNAME.columnNumber());
    instrumentNameCol.setCellValue(ngsMeasurementEntry.instrumentName());
    setCellStyle(instrumentNameCol, NGSMeasurementColumns.INSTRUMENTNAME.readOnly());

    var readTypeCol = entry.createCell(NGSMeasurementColumns.SEQUENCINGREADTYPE.columnNumber());
    readTypeCol.setCellValue(ngsMeasurementEntry.readType());
    setCellStyle(readTypeCol, NGSMeasurementColumns.SEQUENCINGREADTYPE.readOnly());

    var libraryKitCol = entry.createCell(NGSMeasurementColumns.LIBRARYKIT.columnNumber());
    libraryKitCol.setCellValue(ngsMeasurementEntry.libraryKit());
    setCellStyle(libraryKitCol, NGSMeasurementColumns.LIBRARYKIT.readOnly());

    var flowCellCol = entry.createCell(NGSMeasurementColumns.FLOWCELL.columnNumber());
    flowCellCol.setCellValue(ngsMeasurementEntry.flowCell());
    setCellStyle(flowCellCol, NGSMeasurementColumns.FLOWCELL.readOnly());

    var runProtocolCol = entry.createCell(NGSMeasurementColumns.RUNPROTOCOL.columnNumber());
    runProtocolCol.setCellValue(ngsMeasurementEntry.runProtocol());
    setCellStyle(runProtocolCol, NGSMeasurementColumns.RUNPROTOCOL.readOnly());

    var poolGroupCol = entry.createCell(NGSMeasurementColumns.POOLGROUP.columnNumber());
    poolGroupCol.setCellValue(ngsMeasurementEntry.samplePoolGroup());
    setCellStyle(poolGroupCol, NGSMeasurementColumns.POOLGROUP.readOnly());

    var indexI7Col = entry.createCell(NGSMeasurementColumns.INDEXI7.columnNumber());
    indexI7Col.setCellValue(ngsMeasurementEntry.indexI7());
    setCellStyle(indexI7Col, NGSMeasurementColumns.INDEXI7.readOnly());

    var indexI5Col = entry.createCell(NGSMeasurementColumns.INDEXI5.columnNumber());
    indexI5Col.setCellValue(ngsMeasurementEntry.indexI5());
    setCellStyle(indexI5Col, NGSMeasurementColumns.INDEXI5.readOnly());

    var commentCol = entry.createCell(NGSMeasurementColumns.COMMENT.columnNumber());
    commentCol.setCellValue(ngsMeasurementEntry.comment());
    setCellStyle(commentCol, NGSMeasurementColumns.COMMENT.readOnly());
  }

  public void setMeasurements(List<NGSMeasurementEntry> measurements) {
    this.measurements.clear();
    this.measurements.addAll(measurements);
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
      Sheet sheet = workbook.createSheet("NGS Measurement Metadata");

      Row header = sheet.createRow(0);
      defineReadOnlyHeaderStyle(workbook);
      defineReadOnlyCellStyle(workbook);
      defineBoldStyle(workbook);
      formatHeader(header);

      int rowCounter = 1;

      for (NGSMeasurementEntry ngsMeasurementEntry : measurements) {
        Row entry = sheet.createRow(rowCounter);
        createMeasurementEntry(ngsMeasurementEntry, entry);
        rowCounter++;
      }

      setAutoWidth(sheet);

      byteArrayOutputStream = new ByteArrayOutputStream();
      workbook.write(byteArrayOutputStream);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new ApplicationException(ErrorCode.GENERAL, null);
    }

    return byteArrayOutputStream.toByteArray();
  }

  @Override
  public String getFileName() {
    return FILE_NAME;
  }

  /**
   * <b>NGS Measurement Columns</b>
   *
   * <p>Enumeration of the columns shown in the file used for NGS measurement registration and edit in the context of measurement file based upload.
   * Provides the name of the header column, the column index and if the column should be set to readOnly in the generated sheet
   * </p>
   */
  enum NGSMeasurementColumns {

    MEASUREMENTCODE("Measurement ID", 0,
        true),
    SAMPLEID("QBiC Sample Id", 1,
        true),
    SAMPLELABEL(
        "Sample label", 2,
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
        false),
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
        false);

    private final String headerName;
    private final int columnNumber;
    private final boolean readOnly;


    NGSMeasurementColumns(String headerName, int columnNumber, boolean readOnly) {
      this.headerName = headerName;
      this.columnNumber = columnNumber;
      this.readOnly = readOnly;
    }

    public String headerName() {
      return headerName;
    }

    public int columnNumber() {
      return columnNumber;
    }

    public boolean readOnly() {
      return readOnly;
    }
  }
}
