package life.qbic.datamanager.templates.measurement;

import static life.qbic.datamanager.templates.XLSXTemplateHelper.createBoldCellStyle;
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
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.datamanager.download.DownloadContentProvider;
import life.qbic.datamanager.parser.measurement.NGSMeasurementRegisterColumn;
import life.qbic.datamanager.templates.Template;
import life.qbic.datamanager.templates.XLSXTemplateHelper;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.measurement.NGSMeasurementMetadata;
import life.qbic.projectmanagement.domain.model.measurement.NGSMeasurement;
import org.apache.poi.ss.usermodel.CellStyle;
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
public class NGSMeasurementRegisterTemplate extends Template implements DownloadContentProvider {

  private static final String NGS_MEASUREMENT_TEMPLATE_DOMAIN_NAME = "Genomics Template";
  private static final String FILE_NAME_SUFFIX = "ngs_measurements.xlsx";
  private static final Logger log = logger(NGSMeasurementRegisterTemplate.class);
  private static final String DEFAULT_FILE_NAME_PREFIX = "QBiC";
  private String fileNamePrefix = DEFAULT_FILE_NAME_PREFIX;
  private static final int DEFAULT_GENERATED_ROW_COUNT = 200;

  private static void setAutoWidth(Sheet sheet) {
    for (int col = 0; col <= NGSMeasurementRegisterColumn.values().length; col++) {
      sheet.autoSizeColumn(col);
    }
  }

  @Override
  public String getDomainName() {
    return NGS_MEASUREMENT_TEMPLATE_DOMAIN_NAME;
  }

  @Override
  public byte[] getContent() {

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
      CellStyle readOnlyCellStyle = createReadOnlyCellStyle(workbook);
      CellStyle readOnlyHeaderStyle = createReadOnlyHeaderCellStyle(workbook);
      CellStyle boldStyle = createBoldCellStyle(workbook);

      Sheet sheet = workbook.createSheet("NGS Measurement Metadata");

      Row header = getOrCreateRow(sheet, 0);
      for (NGSMeasurementRegisterColumn value : NGSMeasurementRegisterColumn.values()) {
        var cell = getOrCreateCell(header, value.columnIndex());
        if (value.isMandatory()) {
          cell.setCellValue(value.headerName() + "*");
        } else {
          cell.setCellValue(value.headerName());
        }
        cell.setCellStyle(boldStyle);
        if (value.isReadOnly()) {
          cell.setCellStyle(readOnlyHeaderStyle);
        }
      }

      var startIndex = 1; // start in row number 2 with index 1 as the header row has number 1 index 0
      // make sure to create the visible sheet first
      Sheet hiddenSheet = workbook.createSheet("hidden");
      Name sequencingReadTypeArea = createOptionArea(hiddenSheet,
          "Sequencing read type", SequencingReadType.getOptions());

      XLSXTemplateHelper.addDataValidation(sheet,
          NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.columnIndex(),
          startIndex,
          NGSMeasurementRegisterColumn.SEQUENCING_READ_TYPE.columnIndex(),
          DEFAULT_GENERATED_ROW_COUNT - 1,
          sequencingReadTypeArea);

      for (NGSMeasurementRegisterColumn column : NGSMeasurementRegisterColumn.values()) {
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
