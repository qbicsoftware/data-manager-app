package life.qbic.datamanager.templates.measurement;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.createBoldCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createLinkHeaderCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createOptionArea;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createReadOnlyCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.createReadOnlyHeaderCellStyle;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateCell;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.getOrCreateRow;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.hideSheet;
import static life.qbic.datamanager.templates.XLSXTemplateHelper.lockSheet;
import static life.qbic.logging.service.LoggerFactory.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.download.DownloadContentProvider;
import life.qbic.datamanager.parser.measurement.NGSMeasurementEditColumn;
import life.qbic.datamanager.templates.XLSXTemplateHelper;
import life.qbic.datamanager.views.projects.project.measurements.NGSMeasurementEntry;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * <b>NGS Measurement Content Provider</b>
 * <p>
 * Implementation of the {@link DownloadContentProvider} providing the content and file name for any
 * files created from {@link NGSMeasurement} and {@link NGSMeasurementMetadata}
 * </p>
 */
public class NGSMeasurementEditTemplate implements DownloadContentProvider {

  private static final String FILE_NAME_SUFFIX = "ngs_measurements.xlsx";
  private static final Logger log = logger(NGSMeasurementEditTemplate.class);
  private final List<NGSMeasurementEntry> measurements = new LinkedList<>();
  private static final String DEFAULT_FILE_NAME_PREFIX = "QBiC";
  private String fileNamePrefix = DEFAULT_FILE_NAME_PREFIX;
  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;

  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= NGSMeasurementEditColumn.values().length; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  private static void writeMeasurementIntoRow(NGSMeasurementEntry ngsMeasurementEntry,
      Row entryRow, CellStyle readOnlyCellStyle) {

    for (NGSMeasurementEditColumn measurementColumn : NGSMeasurementEditColumn.values()) {
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
      if (measurementColumn.isReadOnly()) {
        cell.setCellStyle(readOnlyCellStyle);
      }
    }
    
    
  }

  public void setMeasurements(List<NGSMeasurementEntry> measurements, String fileNamePrefix) {
    this.measurements.clear();
    this.measurements.addAll(measurements);
    this.fileNamePrefix = fileNamePrefix.trim();
  }


  @Override
  public byte[] getContent() {
    if (measurements.isEmpty()) {
      return new byte[0];
    }

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      CellStyle readOnlyCellStyle = createReadOnlyCellStyle(workbook);
      CellStyle readOnlyHeaderStyle = createReadOnlyHeaderCellStyle(workbook);
      CellStyle boldStyle = createBoldCellStyle(workbook);
      CellStyle linkHeaderStyle = createLinkHeaderCellStyle(workbook);

      Sheet sheet = workbook.createSheet("NGS Measurement Metadata");

      Row header = getOrCreateRow(sheet, 0);
      for (NGSMeasurementEditColumn column : NGSMeasurementEditColumn.values()) {
        var cell = getOrCreateCell(header, column.columnIndex());
        if (column.isMandatory()) {
          cell.setCellValue(column.headerName() + "*");
        } else {
          cell.setCellValue(column.headerName());
        }
        cell.setCellStyle(boldStyle);
        if (column.isReadOnly()) {
          cell.setCellStyle(readOnlyHeaderStyle);
        } else if (column.equals(NGSMeasurementEditColumn.ORGANISATION_ID)) {
          CreationHelper creationHelper = workbook.getCreationHelper();
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress("https://ror.org");
          cell.setCellStyle(linkHeaderStyle);
          cell.setHyperlink(hyperlink);
        } else if (column.equals(NGSMeasurementEditColumn.INSTRUMENT)) {
          CreationHelper creationHelper = workbook.getCreationHelper();
          Hyperlink hyperlink = creationHelper.createHyperlink(HyperlinkType.URL);
          hyperlink.setAddress("https://rdm.qbic.uni-tuebingen.de");
          cell.setCellStyle(linkHeaderStyle);
          cell.setHyperlink(hyperlink);
        }
        //add helper to header
        column.getFillHelp().ifPresent(
            helper -> XLSXTemplateHelper.addInputHelper(sheet,
                column.columnIndex(),
                0,
                column.columnIndex(),
                0,
                helper.exampleValue(),
                helper.description())
        );
      }

      var startIndex = 1; // start in row number 2 with index 1 as the header row has number 1 index 0
      int rowIndex = startIndex;
      for (NGSMeasurementEntry measurement : measurements) {
        Row row = getOrCreateRow(sheet, rowIndex);
        writeMeasurementIntoRow(measurement, row, readOnlyCellStyle);
        rowIndex++;
      }

      var generatedRowCount = rowIndex - startIndex;
      assert generatedRowCount == measurements.size() : "all measurements have a corresponding row";

      // make sure to create the visible sheet first
      Sheet hiddenSheet = workbook.createSheet("hidden");
      Name sequencingReadTypeArea = createOptionArea(hiddenSheet,
          "Sequencing read type", SequencingReadType.getOptions());

      XLSXTemplateHelper.addDataValidation(sheet,
          NGSMeasurementEditColumn.SEQUENCING_READ_TYPE.columnIndex(),
          startIndex,
          NGSMeasurementEditColumn.SEQUENCING_READ_TYPE.columnIndex(),
          DEFAULT_GENERATED_ROW_COUNT - 1,
          sequencingReadTypeArea);

      for (NGSMeasurementEditColumn column : NGSMeasurementEditColumn.values()) {
        column.getFillHelp().ifPresent(
            helper -> XLSXTemplateHelper.addInputHelper(sheet,
                column.columnIndex(),
                startIndex,
                column.columnIndex(),
                DEFAULT_GENERATED_ROW_COUNT - 1,
                helper.exampleValue(),
                helper.description())
        );
      }

      setAutoWidth(sheet);
      workbook.setActiveSheet(0);

      lockSheet(hiddenSheet);
      hideSheet(workbook, hiddenSheet);

      workbook.write(byteArrayOutputStream);
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new ApplicationException(ErrorCode.GENERAL, null);
    }
  }

  @Override
  public String getFileName() {
    return String.join("_", fileNamePrefix, FILE_NAME_SUFFIX);
  }

}
