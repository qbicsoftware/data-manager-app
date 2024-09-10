package life.qbic.datamanager.views.projects.project.measurements.download;

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
import org.apache.poi.ss.util.CellReference;
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
  private static final int DEFAULT_GENERATED_ROW_COUNT = 2_000;

  private enum SequencingReadType {
    SINGLE_END("singe-end"),
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

  private static void writeMeasurementIntoRow(NGSMeasurementEntry ngsMeasurementEntry, Row entry) {
    var measureCol = getCellNeverNull(entry, NGSMeasurementColumns.MEASUREMENTCODE.columnNumber());
    measureCol.setCellValue(ngsMeasurementEntry.measurementCode());
    setCellStyle(measureCol, NGSMeasurementColumns.MEASUREMENTCODE.readOnly());

    var sampleIdCol = getCellNeverNull(entry, NGSMeasurementColumns.SAMPLEID.columnNumber());
    sampleIdCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleId());
    setCellStyle(sampleIdCol, NGSMeasurementColumns.SAMPLEID.readOnly());

    var sampleNameCol = getCellNeverNull(entry, NGSMeasurementColumns.SAMPLENAME.columnNumber());
    sampleNameCol.setCellValue(ngsMeasurementEntry.sampleInformation().sampleName());
    setCellStyle(sampleNameCol, NGSMeasurementColumns.SAMPLENAME.readOnly());

    var orgIdCol = getCellNeverNull(entry, NGSMeasurementColumns.ORGANISATIONID.columnNumber());
    orgIdCol.setCellValue(ngsMeasurementEntry.organisationId());
    setCellStyle(orgIdCol, NGSMeasurementColumns.ORGANISATIONID.readOnly());

    var organisationNameCol = getCellNeverNull(entry,
        NGSMeasurementColumns.ORGANISATIONNAME.columnNumber());

    organisationNameCol.setCellValue(ngsMeasurementEntry.organisationName());
    setCellStyle(organisationNameCol, NGSMeasurementColumns.ORGANISATIONNAME.readOnly());

    var facilityCol = getCellNeverNull(entry, NGSMeasurementColumns.FACILITY.columnNumber());
    facilityCol.setCellValue(ngsMeasurementEntry.facility());
    setCellStyle(facilityCol, NGSMeasurementColumns.FACILITY.readOnly);
    var instrumentCol = getCellNeverNull(entry, NGSMeasurementColumns.INSTRUMENT.columnNumber());
    instrumentCol.setCellValue(ngsMeasurementEntry.instrumentCURI());
    setCellStyle(instrumentCol, NGSMeasurementColumns.INSTRUMENT.readOnly());

    var instrumentNameCol = getCellNeverNull(entry,
        NGSMeasurementColumns.INSTRUMENTNAME.columnNumber());
    instrumentNameCol.setCellValue(ngsMeasurementEntry.instrumentName());
    setCellStyle(instrumentNameCol, NGSMeasurementColumns.INSTRUMENTNAME.readOnly());

    var readTypeCol = getCellNeverNull(entry,
        NGSMeasurementColumns.SEQUENCINGREADTYPE.columnNumber());
    readTypeCol.setCellValue(ngsMeasurementEntry.readType());
    setCellStyle(readTypeCol, NGSMeasurementColumns.SEQUENCINGREADTYPE.readOnly());

    var libraryKitCol = getCellNeverNull(entry, NGSMeasurementColumns.LIBRARYKIT.columnNumber());
    libraryKitCol.setCellValue(ngsMeasurementEntry.libraryKit());
    setCellStyle(libraryKitCol, NGSMeasurementColumns.LIBRARYKIT.readOnly());

    var flowCellCol = getCellNeverNull(entry, NGSMeasurementColumns.FLOWCELL.columnNumber());
    flowCellCol.setCellValue(ngsMeasurementEntry.flowCell());
    setCellStyle(flowCellCol, NGSMeasurementColumns.FLOWCELL.readOnly());

    var runProtocolCol = getCellNeverNull(entry, NGSMeasurementColumns.RUNPROTOCOL.columnNumber());
    runProtocolCol.setCellValue(ngsMeasurementEntry.runProtocol());
    setCellStyle(runProtocolCol, NGSMeasurementColumns.RUNPROTOCOL.readOnly());

    var poolGroupCol = getCellNeverNull(entry, NGSMeasurementColumns.POOLGROUP.columnNumber());
    poolGroupCol.setCellValue(ngsMeasurementEntry.samplePoolGroup());
    setCellStyle(poolGroupCol, NGSMeasurementColumns.POOLGROUP.readOnly());

    var indexI7Col = getCellNeverNull(entry, NGSMeasurementColumns.INDEXI7.columnNumber());
    indexI7Col.setCellValue(ngsMeasurementEntry.indexI7());
    setCellStyle(indexI7Col, NGSMeasurementColumns.INDEXI7.readOnly());

    var indexI5Col = getCellNeverNull(entry, NGSMeasurementColumns.INDEXI5.columnNumber());
    indexI5Col.setCellValue(ngsMeasurementEntry.indexI5());
    setCellStyle(indexI5Col, NGSMeasurementColumns.INDEXI5.readOnly());

    var commentCol = getCellNeverNull(entry, NGSMeasurementColumns.COMMENT.columnNumber());
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

  /**
   * Adds values to the sheet and returns the named area where they were added. The named area can
   * later be used by calling {}
   *
   * @param name           the name to choose for the area
   * @param sheet          the sheet where to add the values
   * @param propertyValues the property values to add
   * @return a defined name for a range of cells in the workbook.
   */
  protected static Name addValueListWithName(String name, Sheet sheet,
      PropertyValues propertyValues) {
    Row headerRow = getRowNeverNull(sheet, 0);
    var columnNumber = Math.max(1,
        headerRow.getLastCellNum()); // we want to obtain 1 for the first to come if there are none and not -1 -.-
    var columnIndex = columnNumber - 1;

    // create header cell
    Cell headerRowCell = headerRow.createCell(columnIndex);
    headerRowCell.setCellValue(propertyValues.propertyName());

    for (int i = 0; i < propertyValues.size(); i++) {
      var rowIndex = i + 1; // +1 because of header row
      Row valueRow = getRowNeverNull(sheet, rowIndex);
      valueRow.createCell(columnIndex).setCellValue(propertyValues.get(i));
    }
    var reference = "'%s'!$%s$%s:$%s$%s".formatted( //e.g. 'My Sheet'!$A$1:$E$23
        sheet.getSheetName(),
        CellReference.convertNumToColString(columnIndex),
        1,
        CellReference.convertNumToColString(columnIndex),
        propertyValues.size() + 1
    );
    var namedArea = sheet.getWorkbook().createName();
    namedArea.setNameName(name);
    namedArea.setRefersToFormula(reference);
    return namedArea;
  }

  protected record PropertyValues(String propertyName, List<String> values) {

    public int size() {
      return values.size();
    }

    public String get(int index) {
      return values.get(index);
    }

    public PropertyValues(String propertyName, List<String> values) {
      this.propertyName = propertyName;
      this.values = Collections.unmodifiableList(values);
    }
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
      //TODO hide sheet
      Name sequencingReadTypeArea = addValueListWithName("sequencingReadTypes", hiddenSheet,
          new PropertyValues("Sequencing read type", SequencingReadType.getOptions()));

      Sheet sheet = workbook.createSheet("NGS Measurement Metadata");
      workbook.setSheetOrder(sheet.getSheetName(), 0);

      Row header = getRowNeverNull(sheet, 0);
      for (NGSMeasurementColumns value : NGSMeasurementColumns.values()) {
        var cell = header.createCell(value.columnNumber());
        cell.setCellValue(value.headerName());
        setHeaderStyle(cell, value.readOnly());
      }

      for (int i = 0; i < measurements.size(); i++) {
        var measurement = measurements.get(i);
        var rowIndex = i + 1;
        Row row = getRowNeverNull(sheet, rowIndex);
        writeMeasurementIntoRow(measurement, row);
      }
      //TODO add data validation

      setAutoWidth(sheet);

      byteArrayOutputStream = new ByteArrayOutputStream();
      workbook.write(byteArrayOutputStream);
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new ApplicationException(ErrorCode.GENERAL, null);
    }

    return byteArrayOutputStream.toByteArray();
  }

  private static String getCellValue(Cell cell) {
    return switch (cell.getCellType()) {
      case FORMULA, _NONE, BLANK, ERROR -> "";
      case STRING -> cell.getStringCellValue();
      case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
      case NUMERIC -> Double.toString(cell.getNumericCellValue());
    };
  }

  private static Row getRowNeverNull(Sheet sheet, int index) {
    return Optional.ofNullable(sheet.getRow(index))
        .orElse(sheet.createRow(index));
  }

  private static Cell getCellNeverNull(Row row, int colIndex) {
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
    private final int columnNumber;
    private final boolean readOnly;
    private final List<String> allowedValues;


    /**
     * @param headerName    the name in the header
     * @param columnIndex   the index of the column this property is in
     * @param readOnly      is the property read only
     * @param allowedValues a list of allowed values; null if not applicable
     */
    NGSMeasurementColumns(String headerName, int columnIndex, boolean readOnly,
        List<String> allowedValues) {
      this.headerName = headerName;
      this.columnNumber = columnIndex;
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

    public int columnNumber() {
      return columnNumber;
    }

    public boolean readOnly() {
      return readOnly;
    }

    public List<String> allowedValues() {
      return Optional.ofNullable(allowedValues).orElse(Collections.emptyList());
    }
  }
}
